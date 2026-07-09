package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import com.rossomak.flashcards.core.domain.usecase.base.NoParamUseCase
import javax.inject.Inject

class CheckVoiceGradingEntitlementUseCase @Inject constructor(private val voiceAnswerGradingRepository: VoiceAnswerGradingRepository) :
    NoParamUseCase<Result<Boolean>> {

    override suspend fun invoke(): Result<Boolean> = voiceAnswerGradingRepository.checkEntitlement()
}
