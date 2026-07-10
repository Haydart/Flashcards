package com.rossomak.flashcards.feature.study.voice

data class VoiceCard(
    val spokenQuestion: String,
    val spokenAnswer: String,
    // Grading context for voice answering: the original (non-speech-transformed) card content
    // the LLM grades the spoken answer against.
    val cardId: String = "",
    val questionText: String = "",
    val answerText: String = "",
)
