package com.rossomak.flashcards.feature.study.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.model.SessionResult
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
     * and shows nothing. Either way [state] is untouched: the displayed results never depend on
     * whether the commit has actually landed.
     */
    private fun commitSession() {
        viewModelScope.launch {
            commitStudySession(result) { onCommitRejected() }
                .onFailure { onCommitRejected() }
        }
    }

    private fun onCommitRejected() {
        _messages.tryEmit(StudySessionSummaryMessage.SaveFailed)
    }
}
