package com.rossomak.flashcards.feature.study.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.model.FlashcardRating
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.UserPreference.VoiceAnswerConsent as VoiceAnswerConsentPreference
import com.rossomak.flashcards.core.domain.model.VoiceOption
import com.rossomak.flashcards.core.domain.model.VoiceSettings as SavedVoiceSettings
import com.rossomak.flashcards.core.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.core.domain.usecase.ObserveUserPreferencesUseCase
import com.rossomak.flashcards.core.domain.usecase.SaveUserPreferenceUseCase
import com.rossomak.flashcards.core.domain.usecase.SubmitCurationReportUseCase
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.navigation.decodeRoute
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.core.ui.voice.toVoiceSettings
import com.rossomak.flashcards.feature.study.StudySessionRoute
import com.rossomak.flashcards.feature.study.session.StudySessionDialog.ExitSession
import com.rossomak.flashcards.feature.study.session.StudySessionDialog.ExtendedContext
import com.rossomak.flashcards.feature.study.session.StudySessionDialog.ReportProblem
import com.rossomak.flashcards.feature.study.session.StudySessionDialog.VoiceAnswerConsent
import com.rossomak.flashcards.feature.study.session.StudySessionDialog.VoiceSettings
import com.rossomak.flashcards.feature.study.voice.VoiceAnswerPhase
import com.rossomak.flashcards.feature.study.voice.VoiceGateway
import com.rossomak.flashcards.feature.study.voice.VoicePhase
import com.rossomak.flashcards.feature.study.voice.VoicePlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StudySessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFlashcards: GetFlashcardsUseCase,
    private val submitCurationReport: SubmitCurationReportUseCase,
    private val observeUserPreferences: ObserveUserPreferencesUseCase,
    private val saveUserPreference: SaveUserPreferenceUseCase,
    private val voiceGateway: VoiceGateway,
    private val voiceSettingsController: VoiceSettingsController,
) : ViewModel() {

    private val route = savedStateHandle.decodeRoute<StudySessionRoute>()
    private val sessionTitle: String = route.sessionTitle

    private val _state = MutableStateFlow(
        StudySessionScreenState(sessionTitle = sessionTitle, studyMode = route.studyMode)
    )
    val state: StateFlow<StudySessionScreenState> = _state.asStateFlow()

    // Tracks eagerly so rapid toggles don't race against isVoiceActive propagation.
    private var voiceStarted = false

    internal var rewindThresholdMs: Long = VoicePlaybackState.REWIND_THRESHOLD_MS

    private var rewindJob: Job? = null
    private var isPastRewindThreshold = false
    private val eventChannel = Channel<StudySessionDestination>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var lastObservedCardIndex = -1

    private val isExtendedContextDialogOpen: Boolean
        get() = _state.value.activeDialog is ExtendedContext

    // True only when the pause was caused by the dialog intercepting a natural between-card advance.
    // Gates auto-advance on dialog dismiss and changes play-button behavior.
    private var pausedDueToExtendedContext = false
    private var advanceAfterExtendedContextJob: Job? = null

    // True only when opening voice settings paused an in-progress playback; gates resume on close.
    private var pausedForVoiceSettings = false

    private var hasVoiceAnswerConsent = false

    // Session-scoped like the rest of the routed config: a mid-session change updates only this
    // running session unless the user checks "keep as my default" (ADR-0030), so it lives in a
    // plain var rather than being re-read from the controller on every playback start.
    private var sessionVoiceSettings: SavedVoiceSettings = route.voiceSettings

    init {
        loadFlashcards()
        observeVoiceState()
        observeVoiceAnswerState()
        observeVoiceAnswerConsentState()
    }

    // Card selection happens on the Preview Study Session screen (ADR-0004); the session only
    // resolves the routed cardIds to full Flashcards, preserving the routed order.
    private fun loadFlashcards() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val results = coroutineScope {
                route.subcategoryIds
                    .map { subcategoryId -> async { getFlashcards(subcategoryId) } }
                    .awaitAll()
            }
            if (results.any { it.isFailure }) {
                _state.update { it.copy(isLoading = false, error = "Could not load flashcards") }
                return@launch
            }
            val cardsById = results.flatMap { it.getOrThrow() }.associateBy { it.id }
            val sessionCards = route.cardIds.mapNotNull(cardsById::get)
            _state.update {
                it.copy(
                    isLoading = false,
                    flashcards = sessionCards,
                    isVoiceAutoStartPending = route.studyMode == StudyMode.Fast && sessionCards.isNotEmpty(),
                )
            }
            honourRoutedVoiceAnswering(hasCards = sessionCards.isNotEmpty())
        }
    }

    /**
     * The Preview screen's voice-answering choice (ADR-0030) takes effect on entry, running the
     * same consent-then-microphone path the in-session toggle uses. Rated only — Fast mode has no
     * rating step for voice answering to drive (ADR-0025).
     *
     * Consent is read as a one-shot rather than from [hasVoiceAnswerConsent], whose collector may
     * not have emitted yet by the time the cards land.
     */
    private suspend fun honourRoutedVoiceAnswering(hasCards: Boolean) {
        if (!route.voiceAnsweringEnabled || route.studyMode != StudyMode.Rated || !hasCards) return
        requestVoiceAnswering(observeUserPreferences().first().voiceAnswerConsentGranted)
    }

    private fun observeVoiceState() {
        viewModelScope.launch {
            voiceGateway.state.collect { voice ->
                if (voice.error != null) {
                    voiceStarted = false
                    _state.update { it.copy(isVoiceActive = false, isVoicePlaying = false, voiceError = voice.error) }
                    return@collect
                }
                _state.update {
                    it.copy(
                        isVoiceActive = voice.isActive,
                        isVoicePlaying = voice.isPlaying,
                        speechRate = voice.speechRate,
                        currentCardIndex = if (voice.isActive) voice.currentIndex else it.currentCardIndex,
                        // Grading/feedback also reveals the card (see observeVoiceAnswerState) —
                        // don't let this collector's phase check stomp that back to false while
                        // the TTS engine itself is still sitting on QUESTION.
                        isAnswerRevealed = if (voice.isActive) {
                            voice.phase == VoicePhase.Answer ||
                                it.voiceAnswerPhase == VoiceAnswerPhase.Grading ||
                                it.voiceAnswerPhase == VoiceAnswerPhase.SpeakingNotice
                        } else {
                            it.isAnswerRevealed
                        },
                    )
                }
                if (voice.isActive && voice.currentIndex != lastObservedCardIndex) {
                    lastObservedCardIndex = voice.currentIndex
                    advanceAfterExtendedContextJob?.cancel()
                    pausedDueToExtendedContext = false
                    startRewindThresholdTimer()
                } else if (!voice.isActive) {
                    voiceStarted = false
                    lastObservedCardIndex = -1
                    advanceAfterExtendedContextJob?.cancel()
                    pausedDueToExtendedContext = false
                    rewindJob?.cancel()
                    isPastRewindThreshold = false
                }
                if (voice.isInBetweenPause && voice.isPlaying && isExtendedContextDialogOpen && !pausedDueToExtendedContext) {
                    pausedDueToExtendedContext = true
                    viewModelScope.launch { voiceGateway.togglePlayPause() }
                }
            }
        }
    }

    private fun observeVoiceAnswerState() {
        viewModelScope.launch {
            voiceGateway.voiceAnswerState.collect { voiceAnswer ->
                _state.update {
                    it.copy(
                        isVoiceAnswerEnabled = voiceAnswer.isEnabled,
                        voiceAnswerPhase = voiceAnswer.phase,
                        voiceAnswerSanitizedTranscript = voiceAnswer.sanitizedTranscript,
                        lastVoiceAnswerGrade = voiceAnswer.lastGrade,
                        voiceAnswerError = voiceAnswer.error,
                        // Grading starts as soon as the utterance is captured, before the TTS
                        // engine's own phase would flip to ANSWER — reveal the card now so the
                        // user can check what they missed while grading/feedback plays out.
                        isAnswerRevealed = it.isAnswerRevealed ||
                            voiceAnswer.phase == VoiceAnswerPhase.Grading,
                    )
                }
            }
        }
    }

    private fun observeVoiceAnswerConsentState() {
        viewModelScope.launch {
            observeUserPreferences().map { it.voiceAnswerConsentGranted }.collect { hasConsent ->
                hasVoiceAnswerConsent = hasConsent
            }
        }
    }

    // Voice answering is Rated-only (ADR-0025) — Fast mode has no rating step for it to drive.
    fun onVoiceAnswerToggle() {
        if (_state.value.studyMode != StudyMode.Rated) return
        if (_state.value.isVoiceAnswerEnabled) {
            // Voice-answering-on drives the shared TTS engine in a stop-after-question shape;
            // there is no meaningful "keep reading, just stop grading" middle state (ADR-0025),
            // so disabling it tears down the whole engine back to manual Show Answer/Next.
            voiceGateway.stop()
            return
        }
        requestVoiceAnswering(hasVoiceAnswerConsent)
    }

    /** Consent first, then the microphone. Both gates are one-time; neither is skippable. */
    private fun requestVoiceAnswering(hasConsent: Boolean) {
        if (hasConsent) {
            _state.update { it.copy(isMicPermissionRequestPending = true) }
        } else {
            _state.update { it.copy(activeDialog = VoiceAnswerConsent) }
        }
    }

    private fun onVoiceAnswerConsentAccept() {
        viewModelScope.launch {
            saveUserPreference(VoiceAnswerConsentPreference(true))
            _state.update {
                it.copy(
                    activeDialog = null,
                    isMicPermissionRequestPending = true,
                )
            }
        }
    }

    fun onMicPermissionResult(isGranted: Boolean) {
        _state.update { it.copy(isMicPermissionRequestPending = false) }
        if (!isGranted) return
        // Rated sessions never auto-start the gateway (only Fast does, via onVoiceAutoStart);
        // enabling voice answering is what bootstraps it here (ADR-0025).
        ensureVoiceGatewayStarted()
        voiceGateway.setVoiceAnswering(true)
    }

    fun onVoiceAnswerGradeDismissed() {
        _state.update { it.copy(lastVoiceAnswerGrade = null) }
    }

    fun onShowAnswer() {
        if (_state.value.isVoiceActive) {
            voiceGateway.showAnswer()
        } else {
            _state.update { it.copy(isAnswerRevealed = true) }
        }
    }

    fun onNextCard() {
        val currentState = _state.value
        if (currentState.currentCardIndex >= currentState.flashcards.lastIndex) {
            navigateBack()
        } else {
            _state.update {
                it.copy(
                    currentCardIndex = it.currentCardIndex + 1,
                    isAnswerRevealed = false,
                )
            }
        }
    }

    fun onRating(rating: FlashcardRating) {
        onNextCard()
    }

    fun onVoiceAutoStartDeclined() {
        _state.update { it.copy(isVoiceAutoStartPending = false) }
    }

    fun onVoiceAutoStart() {
        _state.update { it.copy(isVoiceAutoStartPending = false) }
        ensureVoiceGatewayStarted()
    }

    private fun ensureVoiceGatewayStarted() {
        if (voiceStarted) return
        with(_state.value) {
            if (flashcards.isEmpty()) return
            voiceStarted = true
            voiceGateway.start(
                cards = flashcards,
                startIndex = currentCardIndex,
                subcategoryName = sessionTitle,
            )
        }
        voiceGateway.setSpeechRate(sessionVoiceSettings.speechRate)
        voiceGateway.setVoice(sessionVoiceSettings.voiceId)
    }

    fun onVoicePlayPause() {
        if (pausedDueToExtendedContext) {
            advanceAfterExtendedContextJob?.cancel()
            pausedDueToExtendedContext = false
            viewModelScope.launch {
                voiceGateway.rewindToNext()
                voiceGateway.togglePlayPause()
            }
        } else {
            voiceGateway.togglePlayPause()
        }
    }

    fun onVoiceNext() {
        advanceAfterExtendedContextJob?.cancel()
        pausedDueToExtendedContext = false
        voiceGateway.rewindToNext()
    }

    fun onVoicePrevious() {
        advanceAfterExtendedContextJob?.cancel()
        pausedDueToExtendedContext = false
        if (isPastRewindThreshold || voiceGateway.state.value.currentIndex == 0) {
            voiceGateway.restartCurrentCard()
            startRewindThresholdTimer()
        } else {
            voiceGateway.rewindToPrevious()
        }
    }

    fun onVoiceSpeedChange(rate: Float) {
        voiceGateway.setSpeechRate(rate)
    }

    private fun onExtendedContextDialogOpen(dialog: ExtendedContext) {
        _state.update { it.copy(activeDialog = dialog) }
        val voiceState = voiceGateway.state.value
        if (voiceState.isInBetweenPause && voiceState.isPlaying) {
            pausedDueToExtendedContext = true
            viewModelScope.launch { voiceGateway.togglePlayPause() }
        }
    }

    private fun onExtendedContextDialogDismissed() {
        if (pausedDueToExtendedContext) {
            advanceAfterExtendedContextJob = viewModelScope.launch {
                delay(EXTENDED_CONTEXT_ADVANCE_DELAY_MS)
                pausedDueToExtendedContext = false
                voiceGateway.rewindToNext()
                voiceGateway.togglePlayPause()
            }
        }
    }

    fun onVoiceErrorDismissed() {
        _state.update { it.copy(voiceError = null) }
    }

    private fun startRewindThresholdTimer() {
        rewindJob?.cancel()
        isPastRewindThreshold = false
        rewindJob = viewModelScope.launch {
            delay(rewindThresholdMs)
            isPastRewindThreshold = true
        }
    }

    private fun onVoiceSettingsOpen() {
        if (_state.value.isVoicePlaying) {
            pausedForVoiceSettings = true
            voiceGateway.togglePlayPause()
        }
        _state.update {
            it.copy(activeDialog = VoiceSettings(voiceSettingsController.seedDraft(sessionVoiceSettings)))
        }
        voiceSettingsController.loadVoices(viewModelScope, ::onVoicesLoaded)
    }

    /**
     * The voice list arrives after the dialog is already up, so it has to find the open dialog to
     * fill in — the one narrowing cast left in the dialog path, once per open rather than once per
     * edit. A dismissal in the meantime correctly drops it.
     */
    private fun onVoicesLoaded(voices: List<VoiceOption>) {
        _state.update { state ->
            val dialog = state.activeDialog as? VoiceSettings ?: return@update state
            state.copy(
                activeDialog = dialog.copy(
                    draft = dialog.draft.copy(
                        availableVoices = voices,
                        draftVoiceId = dialog.draft.draftVoiceId ?: voices.firstOrNull()?.id,
                    ),
                ),
            )
        }
    }

    /**
     * Always applies to the rest of this session; only persists as the new default when the
     * dialog's checkbox is checked (ADR-0030). Either way the preview player is done with — a save
     * stops it same as [voiceSettingsController]'s own `save` would, and an unchecked confirm has
     * no other call into the controller left to do that.
     */
    private fun onVoiceSettingsSave() {
        val dialog = _state.value.activeDialog as? VoiceSettings ?: return
        val settings = dialog.draft.toVoiceSettings()
        sessionVoiceSettings = settings
        if (dialog.keepAsDefault) {
            voiceSettingsController.save(viewModelScope, dialog.draft)
        } else {
            voiceSettingsController.stopPreview()
        }
        if (_state.value.isVoiceActive) {
            voiceGateway.setSpeechRate(settings.speechRate)
            voiceGateway.setVoice(settings.voiceId)
        }
        _state.update { it.copy(activeDialog = null) }
        resumeIfPausedForVoiceSettings()
    }

    private fun onVoiceSettingsDismiss() {
        voiceSettingsController.stopPreview()
        _state.update { it.copy(activeDialog = null) }
        resumeIfPausedForVoiceSettings()
    }

    private fun resumeIfPausedForVoiceSettings() {
        if (pausedForVoiceSettings) {
            pausedForVoiceSettings = false
            voiceGateway.togglePlayPause()
        }
    }

    /**
     * Single entry point for every dialog on this screen. Exit-session confirmation is the one
     * case with no ViewModel work behind it — the screen navigates and there is nothing to commit.
     */
    fun onDialogEvent(event: StudySessionDialogEvent) {
        when (event) {
            is Open -> onDialogOpen(event.dialog)
            is DraftChange -> onDraftChange(event.dialog)
            Confirm -> onDialogConfirm()
            Dismiss -> onDialogDismiss()
        }
    }

    /**
     * The caller hands over the dialog it wants shown, already seeded from what it was rendering.
     * This adds only what the call site could not: the playback side effects, and the voice-settings
     * draft, which comes from the shared controller rather than screen state.
     */
    private fun onDialogOpen(dialog: StudySessionDialog) {
        when (dialog) {
            is ReportProblem -> onReportProblemOpen(dialog)
            is ExtendedContext -> onExtendedContextDialogOpen(dialog)
            is VoiceSettings -> onVoiceSettingsOpen()
            VoiceAnswerConsent, ExitSession ->
                _state.update { it.copy(activeDialog = dialog) }
        }
    }

    /**
     * Stores the draft the host built, then fires any side effect the edit implies.
     *
     * The side effect comes from diffing the previous draft against the next rather than from an
     * event that names the changed field: it keeps every dialog on the one generic
     * [StudySessionDialogEvent.DraftChange], and puts the trigger somewhere a unit test can reach
     * (ADR-0036).
     */
    private fun onDraftChange(dialog: StudySessionDialog) {
        val previous = _state.value.activeDialog
        _state.update { it.copy(activeDialog = dialog) }
        if (previous is VoiceSettings &&
            dialog is VoiceSettings &&
            dialog.draft != previous.draft
        ) {
            voiceSettingsController.preview(dialog.draft)
        }
    }

    private fun onDialogConfirm() {
        when (_state.value.activeDialog) {
            is ReportProblem -> onReportProblemSubmit()
            VoiceAnswerConsent -> onVoiceAnswerConsentAccept()
            is VoiceSettings -> onVoiceSettingsSave()
            ExitSession -> {
                onDialogDismiss()
                navigateBack()
            }
            // "Got it" and a scrim tap are the same act on a single-action dialog.
            is ExtendedContext, null -> onDialogDismiss()
        }
    }

    /** Always the discard path: the draft dies with the field. */
    private fun onDialogDismiss() {
        val dialog = _state.value.activeDialog
        _state.update { it.copy(activeDialog = null) }
        when (dialog) {
            is ExtendedContext -> onExtendedContextDialogDismissed()
            is VoiceSettings -> onVoiceSettingsDismiss()
            else -> Unit
        }
    }

    /**
     * Reporting pauses playback the way the old debug FAB did — the user stopped to read the card,
     * not to be read over. Resuming is a deliberate tap (ADR-0017).
     */
    private fun onReportProblemOpen(dialog: ReportProblem) {
        if (_state.value.isVoicePlaying) voiceGateway.togglePlayPause()
        _state.update { it.copy(activeDialog = dialog) }
    }

    private fun onReportProblemSubmit() {
        val dialog = _state.value.activeDialog as? ReportProblem ?: return
        if (!dialog.canSubmit) return
        _state.update { it.copy(activeDialog = null) }
        viewModelScope.launch {
            submitCurationReport(
                SubmitCurationReportUseCase.Params(
                    cardId = dialog.cardId,
                    subcategoryId = dialog.subcategoryId,
                    actions = dialog.selectedActions,
                )
            ).onFailure {
                _state.update { it.copy(curationError = "Failed to submit report") }
            }
        }
    }

    /** Leaving is a one-time event, never a flag in state (ADR-0019). */
    private fun navigateBack() {
        viewModelScope.launch { eventChannel.send(StudySessionDestination.Back) }
    }

    fun onCurationErrorDismissed() {
        _state.update { it.copy(curationError = null) }
    }

    public override fun onCleared() {
        voiceGateway.stop()
        super.onCleared()
    }

    private companion object {
        const val EXTENDED_CONTEXT_ADVANCE_DELAY_MS = 500L
    }
}
