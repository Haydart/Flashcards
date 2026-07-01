package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.VoiceOption

interface VoiceOptionsRepository {
    suspend fun getAvailableVoices(): List<VoiceOption>
}
