package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.repository.VoiceAnswerConsentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveVoiceAnswerConsentUseCase @Inject constructor(
    private val voiceAnswerConsentRepository: VoiceAnswerConsentRepository,
) {

    operator fun invoke(): Flow<Boolean> = voiceAnswerConsentRepository.observeConsent()
}
