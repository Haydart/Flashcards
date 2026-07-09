package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.VoiceOption
import com.rossomak.flashcards.core.domain.repository.VoiceOptionsRepository
import javax.inject.Inject

class GetAvailableVoicesUseCase @Inject constructor(private val repository: VoiceOptionsRepository) {
    suspend operator fun invoke(): List<VoiceOption> = repository.getAvailableVoices()
}
