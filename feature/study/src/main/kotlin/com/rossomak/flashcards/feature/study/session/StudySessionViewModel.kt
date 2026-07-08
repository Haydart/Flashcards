package com.rossomak.flashcards.feature.study.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.model.CurationRequest
import com.rossomak.flashcards.core.domain.model.FlashcardRating
import com.rossomak.flashcards.core.domain.usecase.GetCurationRequestsUseCase
import com.rossomak.flashcards.core.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.core.domain.usecase.ObserveVoiceAnswerConsentUseCase
import com.rossomak.flashcards.core.domain.usecase.SetVoiceAnswerConsentUseCase
import com.rossomak.flashcards.core.domain.usecase.ToggleCurationActionUseCase
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.ui.navigation.decodeRoute
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.feature.study.StudySessionRoute
import com.rossomak.flashcards.feature.study.voice.VoiceAnswerPhase
import com.rossomak.flashcards.feature.study.voice.VoiceGateway
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.rossomak.flashcards.feature.study.voice.VoicePhase
import com.rossomak.flashcards.feature.study.voice.VoicePlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudySessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFlashcards: GetFlashcardsUseCase,
    private val getCurationRequests: GetCurationRequestsUseCase,
    private val toggleCurationAction: ToggleCurationActionUseCase,
    private val observeVoiceAnswerConsent: ObserveVoiceAnswerConsentUseCase,
    private val setVoiceAnswerConsent: SetVoiceAnswerConsentUseCase,
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

    private var curationCacheLoadStarted = false

    internal var rewindThresholdMs: Long = VoicePlaybackState.REWIND_THRESHOLD_MS

    private var rewindJob: Job? = null
    private var isPastRewindThreshold = false
    private var lastObservedCardIndex = -1

    private var isExtendedContextDialogOpen = false

    // True only when the pause was caused by the dialog intercepting a natural between-card advance.
    // Gates auto-advance on dialog dismiss and changes play-button behavior.
    private var pausedDueToExtendedContext = false
    private var advanceAfterExtendedContextJob: Job? = null

    // True only when opening voice settings paused an in-progress playback; gates resume on close.
    private var pausedForVoiceSettings = false

    private var hasVoiceAnswerConsent = false

    init {
        loadFlashcards()
        observeVoiceState()
        observeVoiceAnswerState()
        observeVoiceAnswerConsentState()
        voiceSettingsController.bind(viewModelScope)
        viewModelScope.launch {
            voiceSettingsController.draftState.collect { draft ->
                _state.update { it.copy(voiceSettingsState = draft) }
            }
        }
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
                    isVoiceAutoStartPending = route.studyMode == StudyMode.FAST && sessionCards.isNotEmpty(),
                )
            }
        }
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
                            voice.phase == VoicePhase.ANSWER ||
                                it.voiceAnswerPhase == VoiceAnswerPhase.GRADING ||
                                it.voiceAnswerPhase == VoiceAnswerPhase.SPEAKING_NOTICE
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
                        lastVoiceAnswerGrade = voiceAnswer.lastGrade,
                        voiceAnswerError = voiceAnswer.error,
                        // Grading starts as soon as the utterance is captured, before the TTS
                        // engine's own phase would flip to ANSWER — reveal the card now so the
                        // user can check what they missed while grading/feedback plays out.
                        isAnswerRevealed = it.isAnswerRevealed ||
                            voiceAnswer.phase == VoiceAnswerPhase.GRADING,
                    )
                }
            }
        }
    }

    private fun observeVoiceAnswerConsentState() {
        viewModelScope.launch {
            observeVoiceAnswerConsent().collect { hasConsent ->
                hasVoiceAnswerConsent = hasConsent
            }
        }
    }

    // Voice answering is Rated-only (ADR-0025) — Fast mode has no rating step for it to drive.
    fun onVoiceAnswerToggle() {
        if (_state.value.studyMode != StudyMode.RATED) return
        if (_state.value.isVoiceAnswerEnabled) {
            // Voice-answering-on drives the shared TTS engine in a stop-after-question shape;
            // there is no meaningful "keep reading, just stop grading" middle state (ADR-0025),
            // so disabling it tears down the whole engine back to manual Show Answer/Next.
            voiceGateway.stop()
            return
        }
        if (hasVoiceAnswerConsent) {
            _state.update { it.copy(isMicPermissionRequestPending = true) }
        } else {
            _state.update { it.copy(isVoiceAnswerConsentDialogVisible = true) }
        }
    }

    fun onVoiceAnswerConsentAccept() {
        viewModelScope.launch {
            setVoiceAnswerConsent(true)
            _state.update {
                it.copy(
                    isVoiceAnswerConsentDialogVisible = false,
                    isMicPermissionRequestPending = true,
                )
            }
        }
    }

    fun onVoiceAnswerConsentDecline() {
        _state.update { it.copy(isVoiceAnswerConsentDialogVisible = false) }
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
            _state.update { it.copy(isSessionComplete = true) }
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
        voiceGateway.setSpeechRate(voiceSettingsController.currentSettings.speechRate)
        voiceGateway.setVoice(voiceSettingsController.currentSettings.voiceId)
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

    fun onVoiceSpeedChange(rate: Float) { voiceGateway.setSpeechRate(rate) }

    fun onExtendedContextDialogOpen() {
        isExtendedContextDialogOpen = true
        val voiceState = voiceGateway.state.value
        if (voiceState.isInBetweenPause && voiceState.isPlaying) {
            pausedDueToExtendedContext = true
            viewModelScope.launch { voiceGateway.togglePlayPause() }
        }
    }

    fun onExtendedContextDialogDismissed() {
        isExtendedContextDialogOpen = false
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

    fun onVoiceSettingsCogClick() {
        if (_state.value.isVoicePlaying) {
            pausedForVoiceSettings = true
            voiceGateway.togglePlayPause()
        }
        voiceSettingsController.open(viewModelScope)
    }

    fun onVoiceSettingsDraftVoiceChanged(voiceId: String?) {
        voiceSettingsController.onDraftVoiceChanged(voiceId)
    }

    fun onVoiceSettingsDraftSpeedChanged(speed: Float) {
        voiceSettingsController.onDraftSpeedChanged(speed)
    }

    fun onVoiceSettingsSave() {
        val settings = voiceSettingsController.save(viewModelScope)
        if (_state.value.isVoiceActive) {
            voiceGateway.setSpeechRate(settings.speechRate)
            voiceGateway.setVoice(settings.voiceId)
        }
        resumeIfPausedForVoiceSettings()
    }

    fun onVoiceSettingsDismiss() {
        voiceSettingsController.dismiss()
        resumeIfPausedForVoiceSettings()
    }

    private fun resumeIfPausedForVoiceSettings() {
        if (pausedForVoiceSettings) {
            pausedForVoiceSettings = false
            voiceGateway.togglePlayPause()
        }
    }

    fun onCurationFabClick() {
        if (_state.value.isVoicePlaying) voiceGateway.togglePlayPause()
        if (!curationCacheLoadStarted) {
            curationCacheLoadStarted = true
            loadCurationCache(showDialogOnSuccess = true)
        } else {
            _state.update { it.copy(isCurationDialogVisible = true) }
        }
    }

    private fun loadCurationCache(showDialogOnSuccess: Boolean = false) {
        viewModelScope.launch {
            val cardIds = _state.value.flashcards.map { it.id }
            getCurationRequests(cardIds)
                .onSuccess { requests ->
                    _state.update {
                        it.copy(
                            curationRequests = requests,
                            isCurationDialogVisible = it.isCurationDialogVisible || showDialogOnSuccess,
                        )
                    }
                }
                .onFailure {
                    curationCacheLoadStarted = false
                    _state.update { it.copy(curationError = "Failed to load curation requests") }
                }
        }
    }

    fun onCurationActionToggle(action: CurationAction) {
        val currentCard = _state.value.flashcards.getOrNull(_state.value.currentCardIndex) ?: return
        val currentRequest = _state.value.curationRequests[currentCard.id]
        val isCurrentlyActive = currentRequest?.actions?.containsKey(action) == true

        val updatedActions = (currentRequest?.actions ?: emptyMap()).toMutableMap()
        if (isCurrentlyActive) {
            updatedActions.remove(action)
        } else {
            updatedActions[action] = Instant.now()
            action.difficultyOpposite()?.let { updatedActions.remove(it) }
        }

        val updatedRequest = if (updatedActions.isEmpty()) {
            null
        } else {
            CurationRequest(
                cardId = currentCard.id,
                subcategoryId = currentCard.subcategoryId,
                actions = updatedActions,
            )
        }

        val optimisticRequests = _state.value.curationRequests.toMutableMap().apply {
            if (updatedRequest == null) remove(currentCard.id) else put(currentCard.id, updatedRequest)
        }
        _state.update { it.copy(curationRequests = optimisticRequests) }

        viewModelScope.launch {
            toggleCurationAction(
                ToggleCurationActionUseCase.Params(
                    cardId = currentCard.id,
                    subcategoryId = currentCard.subcategoryId,
                    action = action,
                    isCurrentlyActive = isCurrentlyActive,
                )
            ).onFailure {
                val revertedRequests = _state.value.curationRequests.toMutableMap().apply {
                    if (currentRequest == null) remove(currentCard.id) else put(currentCard.id, currentRequest)
                }
                _state.update {
                    it.copy(
                        curationRequests = revertedRequests,
                        curationError = "Failed to save curation request",
                    )
                }
            }
        }
    }

    fun onCurationDialogDismiss() {
        _state.update { it.copy(isCurationDialogVisible = false) }
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
