package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.repository.VoiceAnswerConsentRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

class SetVoiceAnswerConsentUseCase @Inject constructor(
    private val voiceAnswerConsentRepository: VoiceAnswerConsentRepository,
) : UseCase<Boolean, Unit> {

    override suspend fun invoke(params: Boolean) =
        voiceAnswerConsentRepository.setConsent(params)
}
