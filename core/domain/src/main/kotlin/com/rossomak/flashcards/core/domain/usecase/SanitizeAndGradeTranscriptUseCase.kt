package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

class SanitizeAndGradeTranscriptUseCase @Inject constructor(
    private val voiceAnswerGradingRepository: VoiceAnswerGradingRepository,
) : UseCase<SanitizeAndGradeTranscriptUseCase.Params, Result<VoiceAnswerGrade>> {

    data class Params(
        val question: String,
        val expectedAnswer: String,
        val rawTranscript: String,
    )

    override suspend fun invoke(params: Params): Result<VoiceAnswerGrade> = voiceAnswerGradingRepository.sanitizeAndGrade(
        question = params.question,
        expectedAnswer = params.expectedAnswer,
        rawTranscript = params.rawTranscript
    )
}
