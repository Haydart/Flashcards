package com.rossomak.flashcards.feature.study.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.usecase.SelectSessionFlashcardsUseCase
import com.rossomak.flashcards.core.ui.dialog.DialogEvent
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.navigation.decodeRoute
import com.rossomak.flashcards.feature.study.PreviewStudySessionRoute
import com.rossomak.flashcards.feature.study.StudySessionRoute
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Attempts
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Filters
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Length
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Mode
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.ReadAloud
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Sort
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.VoiceAnswering
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PreviewStudySessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectSessionFlashcards: SelectSessionFlashcardsUseCase,
) : ViewModel() {

    private val route = savedStateHandle.decodeRoute<PreviewStudySessionRoute>()

    private val _state = MutableStateFlow(
        PreviewStudySessionScreenState(
            categoryName = route.categoryName,
            subcategoryNames = route.subcategoryNames,
            isQuickSession = route.isQuickSession,
            config = StudySessionConfig(
                subcategoryIds = route.subcategoryIds,
                tagIds = route.filterTagIds.toSet(),
                seed = Random.nextLong(),
            ),
        )
    )
    val state: StateFlow<PreviewStudySessionScreenState> = _state.asStateFlow()

    private val eventChannel = Channel<PreviewStudySessionDestination>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var sessionStartInFlight = false

    internal var selectedCardIds: List<String> = emptyList()
        private set

    init {
        selectCards(showLoading = true)
    }

    fun onRetry() {
        selectCards(showLoading = true)
    }

    /** A different draw from the same pool — selection is a pure function of the config's seed. */
    fun onRerandomize() {
        _state.update { it.copy(config = it.config.copy(seed = Random.nextLong())) }
        selectCards()
    }

    /**
     * Single entry point for every dialog on this screen.
     *
     * Opening and editing land on the same assignment here because no dialog on this screen has a
     * side effect on open; they stay separate cases in [DialogEvent] for the screens that do.
     */
    fun onDialogEvent(event: PreviewDialogEvent) {
        when (event) {
            is Open -> _state.update { it.copy(activeDialog = event.dialog) }
            is DraftChange -> _state.update { it.copy(activeDialog = event.dialog) }
            Confirm -> onDialogConfirm()
            Dismiss -> onDialogDismiss()
        }
    }

    /** Dismissal is the discard path: the draft dies with the field, so nothing is applied. */
    private fun onDialogDismiss() {
        _state.update { it.copy(activeDialog = null) }
    }

    /**
     * The only dialog state commit path. Every dialog does the same three things: fold the draft into the
     * session config, persist it as a global default if the user asked, and close.
     *
     * Selection re-runs on every confirm regardless of "keep as my default" — the header reads
     * "18 cards · ~12 min", and length, filters and sort all move it.
     */
    private fun onDialogConfirm() {
        val dialog = _state.value.activeDialog ?: return
        val updatedConfig = with(_state.value.config) {
            when (dialog) {
                is Mode -> copy(mode = dialog.draft)
                is VoiceAnswering -> copy(voiceAnsweringEnabled = dialog.draft)
                is Attempts -> copy(ratedAttempts = dialog.draft)
                is ReadAloud -> copy(readAloudEnabled = dialog.draft)
                is Length -> copy(length = dialog.draft)
                is Sort -> copy(sortOrder = dialog.draft)
                is Filters -> copy(
                    tagIds = dialog.draft.selectedTags,
                    difficultyRange = dialog.draft.difficultyRange,
                )
            }
        }
        // TODO(dialog-system Gap 1): where the dialog carries keepAsDefault == true, persist the
        //  value as a global default via SetDefaultStudyModeUseCase / SetDefaultSortOrderUseCase /
        //  SetDefaultSessionLengthUseCase once StudyPreferencesRepository exists (§10 of
        //  docs/temp/dialog-system-plan.md). Until then the opt-in is session-scoped like the value
        //  it sits under: confirm still applies the draft, it just does not outlive the screen.
        _state.update { it.copy(config = updatedConfig, activeDialog = null) }
        selectCards()
    }

    fun onStartSession() {
        if (selectedCardIds.isEmpty() || sessionStartInFlight) return
        sessionStartInFlight = true
        viewModelScope.launch {
            eventChannel.send(
                PreviewStudySessionDestination.StudySession(
                    StudySessionRoute(
                        categoryId = route.categoryId,
                        sessionTitle = sessionTitle(),
                        subcategoryIds = route.subcategoryIds,
                        cardIds = selectedCardIds,
                        studyMode = _state.value.config.mode,
                        voiceAnsweringEnabled = _state.value.config.voiceAnsweringEnabled,
                        ratedAttempts = _state.value.config.ratedAttempts,
                        readAloudEnabled = _state.value.config.readAloudEnabled,
                    )
                )
            )
        }
    }

    private fun selectCards(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) _state.update { it.copy(isLoading = true, error = null) }
            selectSessionFlashcards(_state.value.config)
                .onSuccess { plan ->
                    selectedCardIds = plan.cards.map { it.id }
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            error = null,
                            selectedCardCount = plan.cards.size,
                            estimatedMinutes = plan.estimatedMinutes,
                            // Tags belong to one subcategory, so a multi-topic session has no
                            // coherent tag vocabulary to offer (ADR-0030).
                            availableTags = if (state.isSingleTopic) plan.poolTags else emptyList(),
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, error = "Could not load flashcards") }
                }
        }
    }

    private fun sessionTitle(): String =
        if (_state.value.isSingleTopic) route.subcategoryNames.first() else route.categoryName
}
