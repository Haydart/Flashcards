package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

class TranscribeVoiceClipUseCase @Inject constructor(private val voiceAnswerGradingRepository: VoiceAnswerGradingRepository) :
    UseCase<ByteArray, Result<String>> {

    override suspend fun invoke(params: ByteArray): Result<String> = voiceAnswerGradingRepository.transcribe(params)
}
