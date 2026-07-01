package com.rossomak.flashcards.feature.study.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.model.CurationRequest
import com.rossomak.flashcards.core.domain.usecase.GetCurationRequestsUseCase
import com.rossomak.flashcards.core.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.core.domain.usecase.ToggleCurationActionUseCase
import com.rossomak.flashcards.feature.study.voice.VoiceGateway
import java.time.Instant
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
    private val voiceGateway: VoiceGateway,
) : ViewModel() {

    private val subcategoryId: String = checkNotNull(savedStateHandle["subcategoryId"])
    private val subcategoryName: String = checkNotNull(savedStateHandle["subcategoryName"])

    private val _state = MutableStateFlow(StudySessionScreenState(subcategoryName = subcategoryName))
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

    init {
        loadFlashcards()
        observeVoiceState()
    }

    private fun loadFlashcards() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getFlashcards(subcategoryId)
                .onSuccess { flashcards ->
                    val sampleSize = minOf((flashcards.size * 0.6).toInt(), 150).coerceAtLeast(1)
                    val sampled = flashcards.shuffled().take(sampleSize)
                        .groupBy { it.difficulty }
                        .toSortedMap()
                        .values
                        .flatMap { group -> group.shuffled() }
                    _state.update { it.copy(isLoading = false, flashcards = sampled) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, error = "Could not load flashcards") }
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
                        isAnswerRevealed = if (voice.isActive) voice.phase == VoicePhase.ANSWER else it.isAnswerRevealed,
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

    fun onToggleVoiceMode() {
        if (_state.value.flashcards.isEmpty()) return
        if (voiceStarted) {
            voiceStarted = false
            voiceGateway.stop()
        } else {
            voiceStarted = true
            voiceGateway.start(
                cards = _state.value.flashcards,
                startIndex = _state.value.currentCardIndex,
                subcategoryName = subcategoryName,
            )
        }
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
