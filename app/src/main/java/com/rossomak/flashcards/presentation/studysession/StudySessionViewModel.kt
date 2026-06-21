package com.rossomak.flashcards.presentation.studysession

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.domain.voice.VoiceGateway
import com.rossomak.flashcards.domain.voice.VoicePhase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val voiceGateway: VoiceGateway,
) : ViewModel() {

    private val subcategoryId: String = checkNotNull(savedStateHandle["subcategoryId"])
    private val subcategoryName: String = checkNotNull(savedStateHandle["subcategoryName"])

    private val _state = MutableStateFlow(StudySessionScreenState(subcategoryName = subcategoryName))
    val state: StateFlow<StudySessionScreenState> = _state.asStateFlow()

    // Tracks eagerly so rapid toggles don't race against isVoiceActive propagation.
    private var voiceStarted = false

    init {
        loadFlashcards()
        observeVoiceState()
    }

    private fun loadFlashcards() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getFlashcards(subcategoryId)
                .onSuccess { flashcards ->
                    val sampleSize = minOf((flashcards.size * 0.6).toInt(), 150)
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

    fun onVoicePlayPause() { voiceGateway.togglePlayPause() }
    fun onVoiceNext() { voiceGateway.skipNext() }
    fun onVoicePrevious() { voiceGateway.skipPrevious() }
    fun onVoiceSpeedChange(rate: Float) { voiceGateway.setSpeechRate(rate) }

    fun onVoiceErrorDismissed() {
        _state.update { it.copy(voiceError = null) }
    }

    public override fun onCleared() {
        voiceGateway.stop()
        super.onCleared()
    }
}
