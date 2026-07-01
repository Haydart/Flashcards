package com.rossomak.flashcards.core.domain.repository

interface VoicePreviewGateway {
    fun preview(voiceId: String?, speechRate: Float)
    fun stop()
}
