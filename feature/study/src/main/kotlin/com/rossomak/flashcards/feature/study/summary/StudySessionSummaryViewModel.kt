package com.rossomak.flashcards.feature.study.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.rossomak.flashcards.core.ui.navigation.decodeRoute
import com.rossomak.flashcards.feature.study.StudySessionSummaryRoute
import com.rossomak.flashcards.feature.study.toSessionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads the terminated session's result straight from the route arguments — the only load path this
 * route ever carries (spec 03 ticket 02: fresh-session egress only, never a past session). There is
 * nothing to load asynchronously and nothing to commit; that lands with spec 04.
 */
@HiltViewModel
class StudySessionSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val result = savedStateHandle.decodeRoute<StudySessionSummaryRoute>().toSessionResult()

    private val _state = MutableStateFlow(
        StudySessionSummaryScreenState(
            mode = result.mode,
            durationSeconds = result.durationSeconds,
            studiedCount = result.studiedCount,
            abandoned = result.abandoned,
            masteredCount = result.masteredCount,
            partialCount = result.partialCount,
            failedCount = result.failedCount,
        ),
    )
    val state: StateFlow<StudySessionSummaryScreenState> = _state.asStateFlow()
}
