package com.rossomak.flashcards.core.domain.model

data class VoiceSettings(
    val speechRate: Float = 1f,
    val voiceId: String? = null,
)
