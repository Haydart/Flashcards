package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.VoiceSettings
import com.rossomak.flashcards.core.domain.repository.VoiceSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveVoiceSettingsUseCase @Inject constructor(
    private val repository: VoiceSettingsRepository,
) {
    operator fun invoke(): Flow<VoiceSettings> = repository.voiceSettings()
}
