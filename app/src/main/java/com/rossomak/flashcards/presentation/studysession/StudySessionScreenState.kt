package com.rossomak.flashcards.presentation.studysession

import com.rossomak.flashcards.domain.model.Flashcard
import com.rossomak.flashcards.service.VoicePlaybackState

data class StudySessionScreenState(
    val subcategoryName: String = "",
    val isLoading: Boolean = false,
    val flashcards: List<Flashcard> = emptyList(),
    val currentCardIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val isSessionComplete: Boolean = false,
    val error: String? = null,
    val isVoiceActive: Boolean = false,
    val isVoicePlaying: Boolean = false,
    val speechRate: Float = VoicePlaybackState.DEFAULT_SPEECH_RATE,
    val voiceError: String? = null,
)
