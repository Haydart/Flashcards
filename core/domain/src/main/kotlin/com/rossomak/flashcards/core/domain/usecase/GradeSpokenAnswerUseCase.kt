package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.VoiceAnswerGradingEvent
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GradeSpokenAnswerUseCase @Inject constructor(
    private val voiceAnswerGradingRepository: VoiceAnswerGradingRepository,
) : UseCase<GradeSpokenAnswerUseCase.Params, Flow<VoiceAnswerGradingEvent>> {

    data class Params(
        val cardId: String,
        val question: String,
        val expectedAnswer: String,
        val obfuscatedAnswerWav: ByteArray,
    )

    override suspend fun invoke(params: Params): Flow<VoiceAnswerGradingEvent> =
        voiceAnswerGradingRepository.gradeSpokenAnswer(
            cardId = params.cardId,
            question = params.question,
            expectedAnswer = params.expectedAnswer,
            obfuscatedAnswerWav = params.obfuscatedAnswerWav,
        )
}
