package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.CardProgressEntry
import com.rossomak.flashcards.core.domain.model.CardProgressUpdate
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState.Failed
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState.Mastered
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState.Partial
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState.Seen
import com.rossomak.flashcards.core.domain.model.SessionCommit
import com.rossomak.flashcards.core.domain.model.SessionLedgerEntry
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.SubcategoryProgressWrite
import com.rossomak.flashcards.core.domain.repository.CardProgressRepository
import com.rossomak.flashcards.core.domain.repository.StudySessionRepository
import javax.inject.Inject

/**
 * The single use case that performs the whole session save (ADR-0014), called once by the Summary
 * ViewModel on arrival. Reads each touched Subcategory's prior progress itself
 * ([ADR-0016](../../../../../../../docs/adr/0016-card-progress-model.md)) — never trusting
 * [SessionLedgerEntry.wasPreviouslyMastered], which exists for in-session display only — and turns
 * the session's ledger into the [SubcategoryProgressWrite]s and new-cards-studied count
 * [StudySessionRepository.commitSession] writes alongside the session document, in the same batch.
 *
 * Not a [com.rossomak.flashcards.core.domain.usecase.base.UseCase]: [onRejected] is a side channel
 * for an async, after-the-fact failure report (see [StudySessionRepository.commitSession]), not a
 * second input the single-param `UseCase<P, R>` shape is meant to carry.
 */
class CommitStudySessionUseCase @Inject constructor(
    private val studySessionRepository: StudySessionRepository,
    private val cardProgressRepository: CardProgressRepository,
) {

    suspend operator fun invoke(sessionResult: SessionResult, onRejected: (Throwable) -> Unit = {}): Result<Unit> {
        var newCardsStudied = 0
        val progressWrites = mutableListOf<SubcategoryProgressWrite>()

        for ((subcategoryId, entries) in sessionResult.ledger.groupBy(SessionLedgerEntry::subcategoryId)) {
            val priorCards = cardProgressRepository.getProgress(subcategoryId)
                .getOrElse { exception -> return Result.failure(exception) }
                ?.cards
                .orEmpty()

            val cardUpdates = mutableMapOf<String, CardProgressUpdate>()
            entries.forEach { entry ->
                val prior = priorCards[entry.cardId]
                if (prior == null) newCardsStudied++

                resolveUpdate(sessionResult.mode, entry.state, prior)?.let { update ->
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
        }

        val sessionCommit = SessionCommit(
            sessionResult = sessionResult,
            newCardsStudied = newCardsStudied,
            progressWrites = progressWrites,
        )
        return studySessionRepository.commitSession(sessionCommit, onRejected)
    }

    /**
     * `null` means this card's entry is written unchanged — no batch line for it at all. This is the
     * home of every card-progress write rule in ADR-0016: Fast never touches an existing entry;
     * Rated's Partial is mastery-neutral on an already-mastered card (writes nothing); a Defended
     * card (already Mastered, ending Mastered again) changes no state, so it writes nothing either —
     * only a genuine transition into Mastered stamps [CardProgressUpdate.stampMastered].
     */
    private fun resolveUpdate(
        mode: StudyMode,
        state: FlashcardStudyProgressState,
        prior: CardProgressEntry?,
    ): CardProgressUpdate? = when (mode) {
        StudyMode.Fast -> resolveFastUpdate(prior)
        StudyMode.Rated -> resolveRatedUpdate(state, prior)
    }

    private fun resolveFastUpdate(prior: CardProgressEntry?): CardProgressUpdate? =
        if (prior == null) CardProgressUpdate(state = Seen, stampFirstStudied = true, stampMastered = false) else null

    private fun resolveRatedUpdate(state: FlashcardStudyProgressState, prior: CardProgressEntry?): CardProgressUpdate? {
        if (prior == null) {
            return CardProgressUpdate(state = state, stampFirstStudied = true, stampMastered = state == Mastered)
        }
        val wasMastered = prior.state == Mastered
        return when (state) {
            Mastered -> if (wasMastered) null else CardProgressUpdate(state = Mastered, stampFirstStudied = false, stampMastered = true)
            Partial -> if (wasMastered) null else CardProgressUpdate(state = Partial, stampFirstStudied = false, stampMastered = false)
            Failed -> CardProgressUpdate(state = Failed, stampFirstStudied = false, stampMastered = false)
            Seen -> error("A Rated ledger entry can never resolve to Seen")
        }
    }
}
