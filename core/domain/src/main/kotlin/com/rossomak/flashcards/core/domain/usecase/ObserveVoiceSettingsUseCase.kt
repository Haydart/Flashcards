package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.VoiceSettings
import com.rossomak.flashcards.core.domain.repository.VoiceSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveVoiceSettingsUseCase @Inject constructor(
    private val repository: VoiceSettingsRepository,
) {
    operator fun invoke(): Flow<VoiceSettings> = repository.voiceSettings()
}
