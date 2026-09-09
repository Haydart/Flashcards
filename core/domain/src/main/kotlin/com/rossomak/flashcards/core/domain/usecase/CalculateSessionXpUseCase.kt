package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.ScoringState
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.SessionXpResult
import com.rossomak.flashcards.core.domain.model.XpBreakdown
import com.rossomak.flashcards.core.domain.model.XpConfig
import com.rossomak.flashcards.core.domain.model.levelThreshold
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

/**
 * Spec 05 ticket 02's scoring calculation — pure: no reads, no writes, no dispatchers, no Android.
 * Every rate is read from [SessionResult.xpConfig], the snapshot captured at session start
 * ([ADR-0047](../../../../../../../docs/adr/0047-xp-values-behind-a-config-repository.md)); this use
 * case never touches [com.rossomak.flashcards.core.domain.repository.XpConfigRepository] itself, and
 * no scoring number is a literal here. [Params.newCardsStudied] and [Params.currentState] are the two
 * impure reads [SubmitStudySessionUseCase] performs for its optimistic preview — this unit only ever
 * receives their resolved values, never fetches them itself. Spec 08's `submitStudySession` Cloud
 * Function ports this same calculation to TypeScript for the authoritative, server-side award; this
 * Kotlin copy survives purely as that preview's calculation.
 *
 * The streak and daily-goal awards ([XpBreakdown.streakBonus]/[XpBreakdown.dailyGoalBonus]) are
 * **deliberately never computed here** — spec 05 ticket 03 moved that computation server-side only
 * (`functions/src/lib/xpScoring.ts`'s `computeStreakAndGoalAwards`), so this use case's own preview
 * stays at zero for both, permanently, by design: previewing them accurately would need a new
 * client-side "sum today's sessions" read this codebase has no other reason to have, just to
 * preview-match a line spec 05 ticket 04 (the Summary's animated presentation) hasn't been designed
 * yet. See [ADR-0048](../../../../../../../docs/adr/0048-streak-and-daily-goal-ride-the-session-payload.md)
 * for why a second client-side implementation of the real calculation wasn't built either.
 */
class CalculateSessionXpUseCase @Inject constructor() : UseCase<CalculateSessionXpUseCase.Params, SessionXpResult> {

    /**
     * @param sessionResult the finished session, its own [SessionResult.xpConfig] snapshot included.
     * @param newCardsStudied cards this session touched with no prior progress entry at all, across
     * every Subcategory in scope — [SubmitStudySessionUseCase]'s own estimate, from the same
     * per-Subcategory prior-progress read the old client write path used to also drive persisted
     * card-progress writes; this preview's count can drift from the server's own recount in the rare
     * concurrent-session case spec 08 exists to close (see [SubmitStudySessionUseCase]'s own KDoc).
     * @param currentState the account's [ScoringState] before this session's award is applied.
     */
    data class Params(
        val sessionResult: SessionResult,
        val newCardsStudied: Int,
        val currentState: ScoringState,
    )

    override suspend operator fun invoke(params: Params): SessionXpResult = with(params) {
        val config = sessionResult.xpConfig
        val breakdown = calculateBreakdown(sessionResult, newCardsStudied, config)
        val (newState, levelsCrossed) = applyDelta(currentState, breakdown.xpTotal, config)
        SessionXpResult(breakdown = breakdown, newScoringState = newState, levelsCrossed = levelsCrossed, newCardsStudied = newCardsStudied)
    }

    /**
     * [FlashcardResult.Rated.wasPreviouslyMastered] is trusted directly here, unlike the
     * server-authoritative `submitStudySession` Cloud Function's own progress bookkeeping, which
     * re-reads prior state fresh rather than trust it: that flag is real (stamped from the
     * session-start progress read, ADR-0016), and
     * scoring — unlike a persisted mastery count — tolerates the same mild staleness ADR-0047 already
     * accepts for [XpConfig] itself. It is currently always `false` in practice: card selection never
     * re-draws an already-mastered card into a Rated session, so [XpBreakdown.masteryDefenseBonus] and
     * [XpBreakdown.demastered] compute to zero until spec 07's Mastery Defense gives it a source.
     *
     * A card ending Mastered that was already Mastered earns [XpConfig.masteryDefended] **instead
     * of** [XpConfig.cardMastered], not both: defending is its own, smaller reward, distinct from a
     * fresh mastery, not a bonus stacked on top of it.
     */
    private fun calculateBreakdown(sessionResult: SessionResult, newCardsStudied: Int, config: XpConfig): XpBreakdown {
        val newCards = newCardsStudied * config.newCardStudied
        val timeStudied = (sessionResult.durationSeconds / SECONDS_PER_MINUTE) * config.minuteStudied
        val sessionCompletionBonus = if (sessionResult.abandoned) 0 else config.sessionCompleted
        val cardAwards = if (sessionResult is SessionResult.Rated) calculateRatedCardAwards(sessionResult.cardResults, config) else RatedCardAwards()

        return XpBreakdown(
            newCards = newCards,
            mastered = cardAwards.mastered,
            partial = cardAwards.partial,
            masteryDefenseBonus = cardAwards.masteryDefenseBonus,
            demastered = cardAwards.demastered,
            timeStudied = timeStudied,
            sessionCompletionBonus = sessionCompletionBonus,
        )
    }

    /** [FlashcardResult.Rated]'s share of a breakdown — every award only a Rated ledger can produce. */
    private data class RatedCardAwards(
        val mastered: Int = 0,
        val partial: Int = 0,
        val masteryDefenseBonus: Int = 0,
        val demastered: Int = 0,
    )

    private fun calculateRatedCardAwards(cardResults: List<FlashcardResult.Rated>, config: XpConfig): RatedCardAwards {
        var mastered = 0
        var partial = 0
        var masteryDefenseBonus = 0
        var demastered = 0
        cardResults.forEach { entry ->
            when (entry.state) {
                FlashcardStudyProgressState.Mastered -> {
                    if (entry.wasPreviouslyMastered) masteryDefenseBonus += config.masteryDefended else mastered += config.cardMastered
                }
                FlashcardStudyProgressState.Partial -> partial += config.cardPartial
                FlashcardStudyProgressState.Failed -> if (entry.wasPreviouslyMastered) demastered += config.cardDemastered
                FlashcardStudyProgressState.Seen -> error("A Rated card result can never resolve to Seen")
            }
        }
        return RatedCardAwards(mastered = mastered, partial = partial, masteryDefenseBonus = masteryDefenseBonus, demastered = demastered)
    }

    /**
     * A positive [delta] climbs [ScoringState.level] one [XpConfig.levelThreshold] at a time,
     * reporting each crossing in ascending order, with no burst awarded for reaching one — the leftover
     * past a threshold simply carries into the next level rather than being zeroed. A negative delta is
     * applied as `max(delta, -xpIntoCurrentLevel)`: both [ScoringState.xp] and
     * [ScoringState.xpIntoCurrentLevel] move by that *applied* amount, never enough to push
     * points-into-level below zero or the level below where it already stood.
     */
    private fun applyDelta(state: ScoringState, delta: Int, config: XpConfig): Pair<ScoringState, List<Int>> {
        val appliedDelta = if (delta < 0) maxOf(delta.toLong(), -state.xpIntoCurrentLevel) else delta.toLong()

        var xpIntoCurrentLevel = state.xpIntoCurrentLevel + appliedDelta
        var level = state.level
        val levelsCrossed = mutableListOf<Int>()
        while (xpIntoCurrentLevel >= config.levelThreshold(level)) {
            xpIntoCurrentLevel -= config.levelThreshold(level)
            level += 1
            levelsCrossed += level
        }

        return state.copy(xp = state.xp + appliedDelta, level = level, xpIntoCurrentLevel = xpIntoCurrentLevel) to levelsCrossed
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60
    }
}
