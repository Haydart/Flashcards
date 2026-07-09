package com.rossomak.flashcards.core.data.source

import com.rossomak.flashcards.core.domain.model.VoiceSettings
import kotlinx.coroutines.flow.Flow

interface VoiceSettingsLocalDataSource {

    fun voiceSettings(): Flow<VoiceSettings>

    suspend fun save(settings: VoiceSettings)
}
