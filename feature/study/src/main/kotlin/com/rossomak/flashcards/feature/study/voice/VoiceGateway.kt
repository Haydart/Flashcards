package com.rossomak.flashcards.feature.study.voice

import com.rossomak.flashcards.core.domain.model.Flashcard
import kotlinx.coroutines.flow.StateFlow

enum class VoicePhase { QUESTION, ANSWER }

data class VoicePlaybackState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val currentIndex: Int = 0,
    val totalCards: Int = 0,
    val phase: VoicePhase = VoicePhase.QUESTION,
    val isInBetweenPause: Boolean = false,
    val speechRate: Float = DEFAULT_SPEECH_RATE,
    val error: String? = null,
) {
    companion object {
        const val DEFAULT_SPEECH_RATE = 1f
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 2f
        const val REWIND_THRESHOLD_MS = 3_000L
    }
}

interface VoiceGateway {
    val state: StateFlow<VoicePlaybackState>
    fun start(cards: List<Flashcard>, startIndex: Int, subcategoryName: String)
    fun stop()
    fun togglePlayPause()
    fun rewindToNext()
    fun rewindToPrevious()
    fun restartCurrentCard()
    fun showAnswer()
    fun setSpeechRate(rate: Float)
    fun setVoice(voiceId: String?)
}
