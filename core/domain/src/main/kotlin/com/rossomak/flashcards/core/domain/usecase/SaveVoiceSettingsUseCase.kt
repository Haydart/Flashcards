package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.VoiceSettings
import com.rossomak.flashcards.core.domain.repository.VoiceSettingsRepository
import javax.inject.Inject

class SaveVoiceSettingsUseCase @Inject constructor(
    private val repository: VoiceSettingsRepository,
) {
    suspend operator fun invoke(settings: VoiceSettings) = repository.save(settings)
}
