package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

class GradeSpokenAnswerUseCase @Inject constructor(
    private val voiceAnswerGradingRepository: VoiceAnswerGradingRepository,
) : UseCase<GradeSpokenAnswerUseCase.Params, Result<VoiceAnswerGrade>> {

    data class Params(
        val cardId: String,
        val question: String,
        val expectedAnswer: String,
        val obfuscatedAnswerWav: ByteArray,
    )

    override suspend fun invoke(params: Params): Result<VoiceAnswerGrade> =
        voiceAnswerGradingRepository.gradeSpokenAnswer(
            cardId = params.cardId,
            question = params.question,
            expectedAnswer = params.expectedAnswer,
            obfuscatedAnswerWav = params.obfuscatedAnswerWav,
        )
}
