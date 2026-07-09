package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.source.VoiceOptionsDataSource
import com.rossomak.flashcards.core.domain.model.VoiceOption
import com.rossomak.flashcards.core.domain.repository.VoiceOptionsRepository
import javax.inject.Inject

class DefaultVoiceOptionsRepository @Inject constructor(private val dataSource: VoiceOptionsDataSource) : VoiceOptionsRepository {
    override suspend fun getAvailableVoices(): List<VoiceOption> = dataSource.getAvailableVoices()
}
