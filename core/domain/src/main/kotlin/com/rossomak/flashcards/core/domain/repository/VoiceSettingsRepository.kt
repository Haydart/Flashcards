package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.VoiceSettings
import kotlinx.coroutines.flow.Flow

interface VoiceSettingsRepository {
    fun voiceSettings(): Flow<VoiceSettings>
    suspend fun save(settings: VoiceSettings)
}
