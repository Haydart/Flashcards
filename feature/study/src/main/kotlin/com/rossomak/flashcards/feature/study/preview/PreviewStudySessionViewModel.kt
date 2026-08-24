package com.rossomak.flashcards.feature.study.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.usecase.SelectSessionFlashcardsUseCase
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFilters
import com.rossomak.flashcards.core.ui.navigation.decodeRoute
import com.rossomak.flashcards.feature.study.PreviewStudySessionRoute
import com.rossomak.flashcards.feature.study.StudySessionRoute
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

    /** Single entry point for every dialog on this screen. */
    fun onDialogEvent(event: PreviewDialogEvent) {
        when (event) {
            is PreviewDialogEvent.Open -> onDialogOpen(event)
            is PreviewDialogEvent.DraftChange -> onDraftChange(event)
            PreviewDialogEvent.Confirm -> onDialogConfirm()
            PreviewDialogEvent.Dismiss -> onDialogDismiss()
        }
    }

    /** Every dialog opens seeded from the committed config, never from the last draft. */
    private fun onDialogOpen(event: PreviewDialogEvent.Open) {
        val dialog = with(currentConfig) {
            when (event) {
                PreviewDialogEvent.Open.Mode -> PreviewDialog.Mode(draft = mode)
                PreviewDialogEvent.Open.VoiceAnswering -> PreviewDialog.VoiceAnswering(draft = voiceAnsweringEnabled)
                PreviewDialogEvent.Open.Length -> PreviewDialog.Length(draft = length)
                PreviewDialogEvent.Open.Sort -> PreviewDialog.Sort(draft = sortOrder)
                PreviewDialogEvent.Open.Filters -> PreviewDialog.Filters(
                    draft = FlashcardFilters(selectedTags = tagIds, difficultyRange = difficultyRange),
                )
            }
        }
        _state.update { it.copy(activeDialog = dialog) }
    }

    private fun onDraftChange(event: PreviewDialogEvent.DraftChange) {
        when (event) {
            is PreviewDialogEvent.DraftChange.Mode ->
                updateActiveDialog<PreviewDialog.Mode> { it.copy(draft = event.mode) }
            is PreviewDialogEvent.DraftChange.VoiceAnswering ->
                updateActiveDialog<PreviewDialog.VoiceAnswering> { it.copy(draft = event.isEnabled) }
            is PreviewDialogEvent.DraftChange.Length ->
                updateActiveDialog<PreviewDialog.Length> { it.copy(draft = event.length) }
            is PreviewDialogEvent.DraftChange.SortOrder ->
                updateActiveDialog<PreviewDialog.Sort> { it.copy(draft = event.sortOrder) }
            is PreviewDialogEvent.DraftChange.FilterTag -> updateActiveDialog<PreviewDialog.Filters> { dialog ->
                val selectedTags = if (event.isSelected) {
                    dialog.draft.selectedTags + event.tag
                } else {
                    dialog.draft.selectedTags - event.tag
                }
                dialog.copy(draft = dialog.draft.copy(selectedTags = selectedTags))
            }
            is PreviewDialogEvent.DraftChange.FilterDifficulty -> updateActiveDialog<PreviewDialog.Filters> { dialog ->
                dialog.copy(draft = dialog.draft.copy(difficultyRange = event.difficultyRange))
            }
            is PreviewDialogEvent.DraftChange.KeepAsDefault -> onKeepAsDefaultChange(event.isEnabled)
        }
    }

    private val currentConfig: StudySessionConfig get() = _state.value.config

    /** Dismissal is the discard path: the draft dies with the field, so nothing is applied. */
    private fun onDialogDismiss() {
        _state.update { it.copy(activeDialog = null) }
    }

    private fun onKeepAsDefaultChange(isEnabled: Boolean) {
        _state.update { state ->
            val updatedDialog = when (val dialog = state.activeDialog) {
                is PreviewDialog.Mode -> dialog.copy(keepAsDefault = isEnabled)
                is PreviewDialog.VoiceAnswering -> dialog.copy(keepAsDefault = isEnabled)
                is PreviewDialog.Length -> dialog.copy(keepAsDefault = isEnabled)
                is PreviewDialog.Sort -> dialog.copy(keepAsDefault = isEnabled)
                is PreviewDialog.Filters, null -> return@update state
            }
            state.copy(activeDialog = updatedDialog)
        }
    }

    /**
     * The only commit path. Every dialog does the same three things: fold the draft into the
     * session config, persist it as a global default iff the user asked, and close.
     *
     * Selection re-runs on every confirm regardless of "keep as my default" — the header reads
     * "18 cards · ~12 min", and length, filters and sort all move it.
     */
    private fun onDialogConfirm() {
        val dialog = _state.value.activeDialog ?: return
        val updatedConfig = with(_state.value.config) {
            when (dialog) {
                is PreviewDialog.Mode -> copy(mode = dialog.draft)
                is PreviewDialog.VoiceAnswering -> copy(voiceAnsweringEnabled = dialog.draft)
                is PreviewDialog.Length -> copy(length = dialog.draft)
                is PreviewDialog.Sort -> copy(sortOrder = dialog.draft)
                is PreviewDialog.Filters -> copy(
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

    /**
     * Narrows the open dialog to the case an event belongs to, ignoring the event when it does not
     * match. The cast is the price of one sealed field instead of one nullable field per dialog —
     * an event can only arrive from a dialog that is currently on screen, so a mismatch means a
     * race with dismissal, and dropping it is correct.
     */
    private inline fun <reified T : PreviewDialog> updateActiveDialog(transform: (T) -> T) {
        _state.update { state ->
            val dialog = state.activeDialog as? T ?: return@update state
            state.copy(activeDialog = transform(dialog))
        }
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
