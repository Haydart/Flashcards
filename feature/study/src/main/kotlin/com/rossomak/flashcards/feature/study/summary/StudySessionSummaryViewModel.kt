package com.rossomak.flashcards.feature.study.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.SessionXpResult
import com.rossomak.flashcards.core.domain.model.levelThreshold
import com.rossomak.flashcards.core.domain.usecase.CommitStudySessionUseCase
import com.rossomak.flashcards.core.ui.navigation.decodeRoute
import com.rossomak.flashcards.feature.study.StudySessionSummaryRoute
import com.rossomak.flashcards.feature.study.toSessionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Reads the terminated session's result straight from the route arguments — the only load path this
 * route ever carries (spec 03 ticket 02: fresh-session egress only, never a past session) — and
 * commits it once, on arrival (ADR-0014).
 *
 * The commit fires from `init`, which Hilt/Compose Navigation only run once per back-stack entry:
 * this ViewModel survives configuration change, so there is no separate "have I already committed"
 * flag to maintain. There is likewise no branch to skip a past-session load: [StudySessionSummaryRoute]
 * has no sessionId-only shape today, only ever a complete [SessionResult][com.rossomak.flashcards.core.domain.model.SessionResult] —
 * a future past-session detail view is a separate screen and route (ADR-0014), not a branch of this one.
 */
@HiltViewModel
class StudySessionSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val commitStudySession: CommitStudySessionUseCase,
) : ViewModel() {

    private val result = savedStateHandle.decodeRoute<StudySessionSummaryRoute>().toSessionResult()

    /**
     * 0/0/0 for a Fast result is a UI-state convention only (see [StudySessionSummaryScreenState]'s own
     * KDoc) — the screen already chooses its layout off `mode`, never off these being zero. The domain
     * [SessionResult] itself has no such fields on its Fast branch at all (sealed).
     */
    private val terminalStateCounts: Triple<Int, Int, Int> = when (result) {
        is SessionResult.Rated -> Triple(result.masteredCount, result.partialCount, result.failedCount)
        is SessionResult.Fast -> Triple(0, 0, 0)
    }

    private val _state = MutableStateFlow(
        StudySessionSummaryScreenState(
            mode = result.mode,
            durationSeconds = result.durationSeconds,
            studiedCount = result.studiedCount,
            abandoned = result.abandoned,
            masteredCount = terminalStateCounts.first,
            partialCount = terminalStateCounts.second,
            failedCount = terminalStateCounts.third,
        ),
    )
    val state: StateFlow<StudySessionSummaryScreenState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<StudySessionSummaryMessage>(extraBufferCapacity = 1)

    /** Transient one-shot messages for the snackbar. Never screen state. */
    val messages: SharedFlow<StudySessionSummaryMessage> = _messages.asSharedFlow()

    init {
        commitSession()
    }

    /**
     * A rejected write — synchronous or reported later through [CommitStudySessionUseCase]'s
     * `onRejected` — surfaces the same non-blocking message; a queued offline write reports neither
     * and shows nothing. Either way the counts set at construction are untouched: the displayed
     * results never depend on whether the commit has actually landed.
     *
     * The XP fields are different: computing them needs [CommitStudySessionUseCase]'s own reads
     * (the account's prior scoring state, spec 05 ticket 02), so they can only ever be known once this
     * call resolves. On a genuine failure here — a failed prior-progress or scoring-state read, or a
     * synchronous write rejection — [state]'s XP fields simply stay at their zero defaults alongside
     * the [StudySessionSummaryMessage.SaveFailed] snackbar; a later async rejection (queued-then-
     * rejected) leaves an already-applied breakdown on screen exactly as it leaves the counts above.
     */
    private fun commitSession() {
        viewModelScope.launch {
            commitStudySession(result) { onCommitRejected() }
                .onSuccess { xpResult -> applyXpResult(xpResult) }
                .onFailure { onCommitRejected() }
        }
    }

    private fun applyXpResult(xpResult: SessionXpResult) {
        val config = result.xpConfig
        _state.update {
            it.copy(
                xpLines = buildXpBreakdownLines(result, xpResult),
                xpTotal = xpResult.breakdown.xpTotal,
                level = xpResult.newScoringState.level,
                xpIntoCurrentLevel = xpResult.newScoringState.xpIntoCurrentLevel,
                xpForNextLevel = config.levelThreshold(xpResult.newScoringState.level),
            )
        }
    }

    private fun onCommitRejected() {
        _messages.tryEmit(StudySessionSummaryMessage.SaveFailed)
    }
}

private const val SECONDS_PER_MINUTE = 60

/**
 * The plain itemised breakdown (spec 05 ticket 02): one [XpBreakdownLine] per source [xpResult]
 * actually awarded XP for, in the same order as the awards table, zero-[XpBreakdownLine.amount] sources
 * dropped entirely. [SessionXpResult.newCardsStudied], [SessionResult.Rated.partialCount] and counts
 * derived from `cardResults` here (mirroring [CalculateSessionXpUseCase][com.rossomak.flashcards.core.domain.usecase.CalculateSessionXpUseCase]'s
 * own split between a fresh mastery and a defended one) supply each line's [XpBreakdownLine.count];
 * [XpConfig][com.rossomak.flashcards.core.domain.model.XpConfig]'s rates and [xpResult]'s
 * already-multiplied totals supply the rest — nothing here recomputes an amount.
 */
private fun buildXpBreakdownLines(result: SessionResult, xpResult: SessionXpResult): List<XpBreakdownLine> {
    val config = result.xpConfig
    val minutesStudied = result.durationSeconds / SECONDS_PER_MINUTE
    val lines = mutableListOf(
        XpBreakdownLine(XpAwardSource.NewCards, xpResult.newCardsStudied, config.newCardStudied, xpResult.breakdown.newCards),
    )
    if (result is SessionResult.Rated) {
        // Defended (Mastered again after already being Mastered) earns masteryDefenseBonus instead
        // of mastered, not in addition — so newlyMasteredCount, not result.masteredCount, is what
        // count × rate must reproduce mastered's amount.
        val newlyMasteredCount = result.cardResults.count { it.state == FlashcardStudyProgressState.Mastered && !it.wasPreviouslyMastered }
        val defendedCount = result.cardResults.count { it.state == FlashcardStudyProgressState.Mastered && it.wasPreviouslyMastered }
        val demasteredCount = result.cardResults.count { it.state == FlashcardStudyProgressState.Failed && it.wasPreviouslyMastered }
        lines += XpBreakdownLine(XpAwardSource.Mastered, newlyMasteredCount, config.cardMastered, xpResult.breakdown.mastered)
        lines += XpBreakdownLine(XpAwardSource.Partial, result.partialCount, config.cardPartial, xpResult.breakdown.partial)
        lines += XpBreakdownLine(XpAwardSource.MasteryDefended, defendedCount, config.masteryDefended, xpResult.breakdown.masteryDefenseBonus)
        lines += XpBreakdownLine(XpAwardSource.MasteryLost, demasteredCount, config.cardDemastered, xpResult.breakdown.demastered)
    }
    lines += XpBreakdownLine(XpAwardSource.TimeStudied, minutesStudied, config.minuteStudied, xpResult.breakdown.timeStudied)
    lines += XpBreakdownLine(
        XpAwardSource.SessionCompleted,
        count = if (result.abandoned) 0 else 1,
        rate = config.sessionCompleted,
        amount = xpResult.breakdown.sessionCompletionBonus,
    )
    return lines.filter { it.amount != 0 }
}
