package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.CardProgressEntry
import com.rossomak.flashcards.core.domain.model.CardProgressUpdate
import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState.Failed
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState.Mastered
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState.Partial
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState.Seen
import com.rossomak.flashcards.core.domain.model.ProgressSummaryWrite
import com.rossomak.flashcards.core.domain.model.ScoringState
import com.rossomak.flashcards.core.domain.model.SessionCommit
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.SessionXpResult
import com.rossomak.flashcards.core.domain.model.SubcategoryProgressSummaryDelta
import com.rossomak.flashcards.core.domain.model.SubcategoryProgressWrite
import com.rossomak.flashcards.core.domain.repository.CardProgressRepository
import com.rossomak.flashcards.core.domain.repository.ScoringStateRepository
import com.rossomak.flashcards.core.domain.repository.StudySessionRepository
import javax.inject.Inject

/**
 * The single use case that performs the whole session save (ADR-0014), called once by the Summary
 * ViewModel on arrival. Reads each touched Subcategory's prior progress itself
 * ([ADR-0016](../../../../../../../docs/adr/0016-card-progress-model.md)) — never trusting
 * [FlashcardResult.Rated.wasPreviouslyMastered], which exists for in-session display only — and turns
 * the session's card results into the [SubcategoryProgressWrite]s, the [ProgressSummaryWrite] and the
 * new-cards-studied count [StudySessionRepository.commitSession] writes alongside the session
 * document, in the same batch.
 *
 * Spec 05 ticket 02 adds a further read of the account's prior [ScoringState], then hands
 * [sessionResult], the resulting new-cards-studied count and that prior state to
 * [CalculateSessionXpUseCase] to produce the [SessionXpResult] joining this same commit. A failed
 * [ScoringStateRepository] read fails the whole commit, same as a failed [CardProgressRepository]
 * read below — defaulting a real account's missing read to [ScoringState]'s zero defaults would
 * silently overwrite its accumulated XP; only a genuinely absent document (a brand-new account) may
 * default that way, and [ScoringStateRepository.getScoringState] already encodes that distinction.
 *
 * Not a [com.rossomak.flashcards.core.domain.usecase.base.UseCase]: [onRejected] is a side channel
 * for an async, after-the-fact failure report (see [StudySessionRepository.commitSession]), not a
 * second input the single-param `UseCase<P, R>` shape is meant to carry.
 */
class CommitStudySessionUseCase @Inject constructor(
    private val studySessionRepository: StudySessionRepository,
    private val cardProgressRepository: CardProgressRepository,
    private val scoringStateRepository: ScoringStateRepository,
    private val calculateSessionXp: CalculateSessionXpUseCase,
) {

    suspend operator fun invoke(sessionResult: SessionResult, onRejected: (Throwable) -> Unit = {}): Result<SessionXpResult> {
        var newCardsStudied = 0
        val progressWrites = mutableListOf<SubcategoryProgressWrite>()
        val summaryDeltas = mutableMapOf<String, SubcategoryProgressSummaryDelta>()

        for ((subcategoryId, entries) in sessionResult.cardResults.groupBy(FlashcardResult::subcategoryId)) {
            val priorCards = cardProgressRepository.getProgress(subcategoryId)
                .getOrElse { exception -> return Result.failure(exception) }
                ?.cards
                .orEmpty()

            val cardUpdates = mutableMapOf<String, CardProgressUpdate>()
            var masteredDelta = 0
            var studiedDelta = 0
            entries.forEach { entry ->
                val prior = priorCards[entry.cardId]
                if (prior == null) {
                    newCardsStudied++
                    studiedDelta++
                }
                masteredDelta += resolveMasteredDelta(entry, prior)

                resolveUpdate(entry, prior)?.let { update ->
                    cardUpdates[entry.cardId] = update
                }
            }

            if (cardUpdates.isNotEmpty()) {
                progressWrites += SubcategoryProgressWrite(
                    subcategoryId = subcategoryId,
                    categoryId = sessionResult.categoryId,
                    cards = cardUpdates,
                )
            }
            if (masteredDelta != 0 || studiedDelta != 0) {
                summaryDeltas[subcategoryId] = SubcategoryProgressSummaryDelta(masteredDelta, studiedDelta)
            }
        }

        val currentScoringState = scoringStateRepository.getScoringState()
            .getOrElse { exception -> return Result.failure(exception) }
            ?: ScoringState()
        val xpResult = calculateSessionXp(
            CalculateSessionXpUseCase.Params(
                sessionResult = sessionResult,
                newCardsStudied = newCardsStudied,
                currentState = currentScoringState,
            ),
        )

        val sessionCommit = SessionCommit(
            sessionResult = sessionResult,
            newCardsStudied = newCardsStudied,
            progressWrites = progressWrites,
            progressSummaryWrite = ProgressSummaryWrite(summaryDeltas),
            xpBreakdown = xpResult.breakdown,
            newScoringState = xpResult.newScoringState,
        )
        return studySessionRepository.commitSession(sessionCommit, onRejected).map { xpResult }
    }

    /**
     * `null` means this card's entry is written unchanged — no batch line for it at all. This is the
     * home of every card-progress write rule in ADR-0016: Fast never touches an existing entry;
     * Rated's Partial is mastery-neutral on an already-mastered card (writes nothing); a Defended
     * card (already Mastered, ending Mastered again) changes no state, so it writes nothing either —
     * only a genuine transition into Mastered stamps [CardProgressUpdate.stampMastered].
     */
    private fun resolveUpdate(entry: FlashcardResult, prior: CardProgressEntry?): CardProgressUpdate? = when (entry) {
        is FlashcardResult.Fast -> resolveFastUpdate(prior)
        is FlashcardResult.Rated -> resolveRatedUpdate(entry.state, prior)
    }

    private fun resolveFastUpdate(prior: CardProgressEntry?): CardProgressUpdate? =
        if (prior == null) CardProgressUpdate(state = Seen, stampFirstStudied = true, stampMastered = false) else null

    /**
     * The mastered-count half of ticket 03's progress-summary delta: `+1` for a newly mastered card,
     * `-1` for a de-mastered one, `0` for a defended, Partial or otherwise unchanged card. A Fast
     * result is always `0` — it has no mastery concept to report at all, not a mastery field that
     * happens to stay zero (ADR-0016).
     */
    private fun resolveMasteredDelta(entry: FlashcardResult, prior: CardProgressEntry?): Int = when (entry) {
        is FlashcardResult.Fast -> 0
        is FlashcardResult.Rated -> when (entry.state) {
            Mastered -> if (wasMastered(prior)) 0 else 1
            Failed -> if (wasMastered(prior)) -1 else 0
            Partial -> 0
            Seen -> error("A Rated card result can never resolve to Seen")
        }
    }

    private fun resolveRatedUpdate(state: FlashcardStudyProgressState, prior: CardProgressEntry?): CardProgressUpdate? {
        if (prior == null) {
            return CardProgressUpdate(state = state, stampFirstStudied = true, stampMastered = state == Mastered)
        }
        return when (state) {
            Mastered -> if (wasMastered(prior)) null else CardProgressUpdate(state = Mastered, stampFirstStudied = false, stampMastered = true)
            Partial -> if (wasMastered(prior)) null else CardProgressUpdate(state = Partial, stampFirstStudied = false, stampMastered = false)
            Failed -> CardProgressUpdate(state = Failed, stampFirstStudied = false, stampMastered = false)
            Seen -> error("A Rated card result can never resolve to Seen")
        }
    }

    /** Shared by [resolveRatedUpdate] and [resolveMasteredDelta] so "was this card Mastered before this session touched it" has one definition. */
    private fun wasMastered(prior: CardProgressEntry?): Boolean = prior?.state == Mastered
}
