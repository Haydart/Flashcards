package com.rossomak.flashcards.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VoiceSettings(
    val speechRate: Float = 1f,
    val voiceId: String? = null,
)
