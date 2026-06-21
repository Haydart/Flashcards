package com.rossomak.flashcards.presentation.studysession

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rossomak.flashcards.domain.model.Flashcard
import com.rossomak.flashcards.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.service.StudySessionVoiceService
import com.rossomak.flashcards.service.VoiceCard
import com.rossomak.flashcards.service.VoicePhase
import com.rossomak.flashcards.ui.navigation.StudySession
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudySessionViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val getFlashcards: GetFlashcardsUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<StudySession>()

    private val _state = MutableStateFlow(StudySessionScreenState(subcategoryName = route.subcategoryName))
    val state: StateFlow<StudySessionScreenState> = _state.asStateFlow()

    private var voiceBinder: StudySessionVoiceService.LocalBinder? = null
    private var voiceStateJob: Job? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? StudySessionVoiceService.LocalBinder ?: return
            voiceBinder = binder
            binder.loadSession(
                cards = _state.value.flashcards.toVoiceCards(),
                startIndex = _state.value.currentCardIndex,
                subcategoryName = route.subcategoryName,
            )
            observeVoiceState(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceBinder = null
        }
    }

    init {
        loadFlashcards()
    }

    private fun loadFlashcards() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getFlashcards(route.subcategoryId)
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

    fun onShowAnswer() {
        val binder = voiceBinder
        if (_state.value.isVoiceActive && binder != null) {
            binder.showAnswer()
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
        if (isBound) stopVoice() else startVoice()
    }

    fun onVoicePlayPause() {
        voiceBinder?.togglePlayPause()
    }

    fun onVoiceNext() {
        voiceBinder?.skipNext()
    }

    fun onVoicePrevious() {
        voiceBinder?.skipPrevious()
    }

    fun onVoiceSpeedChange(rate: Float) {
        voiceBinder?.setSpeechRate(rate)
    }

    private fun startVoice() {
        val intent = Intent(appContext, StudySessionVoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
        isBound = appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopVoice() {
        voiceBinder?.stopPlayback()
        unbindVoice()
        _state.update { it.copy(isVoiceActive = false, isVoicePlaying = false) }
    }

    private fun observeVoiceState(binder: StudySessionVoiceService.LocalBinder) {
        voiceStateJob?.cancel()
        voiceStateJob = viewModelScope.launch {
            binder.state.collect { voice ->
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

    private fun unbindVoice() {
        voiceStateJob?.cancel()
        voiceStateJob = null
        if (isBound) {
            runCatching { appContext.unbindService(serviceConnection) }
            isBound = false
        }
        voiceBinder = null
    }

    override fun onCleared() {
        voiceBinder?.stopPlayback()
        unbindVoice()
        super.onCleared()
    }

    private fun List<Flashcard>.toVoiceCards(): List<VoiceCard> = map { card ->
        VoiceCard(
            spokenQuestion = (card.questionSpoken?.takeIf { it.isNotBlank() } ?: card.question).forSpeech(),
            spokenAnswer = (card.answerSpoken?.takeIf { it.isNotBlank() } ?: card.answer).forSpeech(),
        )
    }

    /** Strip inline-code backticks so TTS doesn't read "backtick" aloud. Model text is unchanged. */
    private fun String.forSpeech(): String = replace("`", "")
}
