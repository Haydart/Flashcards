package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.source.VoiceSettingsLocalDataSource
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import com.rossomak.flashcards.core.domain.repository.VoiceSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultVoiceSettingsRepository @Inject constructor(private val localDataSource: VoiceSettingsLocalDataSource) :
    VoiceSettingsRepository {

    override fun voiceSettings(): Flow<VoiceSettings> = localDataSource.voiceSettings()

    override suspend fun save(settings: VoiceSettings) = localDataSource.save(settings)
}
