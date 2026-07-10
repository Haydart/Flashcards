package com.rossomak.flashcards.domain.usecase

import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGradingEvent
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import com.rossomak.flashcards.core.domain.usecase.GradeSpokenAnswerUseCase
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GradeSpokenAnswerUseCaseTest {

    private val voiceAnswerGradingRepository: VoiceAnswerGradingRepository = mockk()
    private val useCase = GradeSpokenAnswerUseCase(voiceAnswerGradingRepository)

    private val params = GradeSpokenAnswerUseCase.Params(
        cardId = "card-1",
        question = "What is a wake lock?",
        expectedAnswer = "A mechanism keeping the CPU awake",
        obfuscatedAnswerWav = byteArrayOf(1, 2, 3),
    )

    @Test
    fun `forwards params to repository and streams both events`() = runTest {
        val grade = VoiceAnswerGrade(sanitizedTranscript = "clean", gradePercent = 90, feedback = "great")
        every {
            voiceAnswerGradingRepository.gradeSpokenAnswer(
                params.cardId,
                params.question,
                params.expectedAnswer,
                params.obfuscatedAnswerWav,
            )
        } returns flow {
            emit(VoiceAnswerGradingEvent.TranscriptReady(grade.sanitizedTranscript))
            emit(VoiceAnswerGradingEvent.Graded(grade))
        }

        useCase(params).test {
            awaitItem() shouldBe VoiceAnswerGradingEvent.TranscriptReady(grade.sanitizedTranscript)
            awaitItem() shouldBe VoiceAnswerGradingEvent.Graded(grade)
            awaitComplete()
        }
        verify(exactly = 1) {
            voiceAnswerGradingRepository.gradeSpokenAnswer(
                params.cardId,
                params.question,
                params.expectedAnswer,
                params.obfuscatedAnswerWav,
            )
        }
    }

    @Test
    fun `propagates repository failure as a flow exception`() = runTest {
        val error = IllegalStateException("backend down")
        every {
            voiceAnswerGradingRepository.gradeSpokenAnswer(any(), any(), any(), any())
        } returns flow { throw error }

        useCase(params).test {
            awaitError() shouldBe error
        }
    }
}
