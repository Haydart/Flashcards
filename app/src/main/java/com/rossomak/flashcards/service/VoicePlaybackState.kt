package com.rossomak.flashcards.service

/**
 * Which part of the current card the voice playback is on. [QUESTION] means the question is
 * being (or about to be) spoken; [ANSWER] means the answer is being (or about to be) spoken and
 * should be revealed on screen.
 */
enum class VoicePhase { QUESTION, ANSWER }

/**
 * The spoken text for a single card, resolved up front by the ViewModel so the service stays
 * free of the [com.rossomak.flashcards.domain.model.Flashcard] domain model. Code blocks are
 * intentionally excluded — only prose is read aloud.
 */
data class VoiceCard(
    val spokenQuestion: String,
    val spokenAnswer: String,
)

/**
 * Snapshot of voice playback the ViewModel observes to drive the study screen and its controls.
 * [isActive] is false when no session is loaded into the service.
 */
data class VoicePlaybackState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val currentIndex: Int = 0,
    val totalCards: Int = 0,
    val phase: VoicePhase = VoicePhase.QUESTION,
    val speechRate: Float = DEFAULT_SPEECH_RATE,
    val error: String? = null,
) {
    companion object {
        const val DEFAULT_SPEECH_RATE = 1f
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 2f
    }
}
