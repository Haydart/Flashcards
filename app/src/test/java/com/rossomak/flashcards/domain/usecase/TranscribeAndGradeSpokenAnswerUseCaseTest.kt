package com.rossomak.flashcards.domain.usecase

import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGradingEvent
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import com.rossomak.flashcards.core.domain.usecase.TranscribeAndGradeSpokenAnswerUseCase
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TranscribeAndGradeSpokenAnswerUseCaseTest {

    private val voiceAnswerGradingRepository: VoiceAnswerGradingRepository = mockk()
    private val useCase = TranscribeAndGradeSpokenAnswerUseCase(voiceAnswerGradingRepository)

    private val params = TranscribeAndGradeSpokenAnswerUseCase.Params(
        cardId = "card-1",
        question = "What is a wake lock?",
        expectedAnswer = "A mechanism keeping the CPU awake",
        obfuscatedAnswerWav = byteArrayOf(1, 2, 3),
    )

    @Test
    fun `forwards params to repository and streams both events`() = runTest {
        val grade = VoiceAnswerGrade(sanitizedTranscript = "clean", gradePercent = 90, feedback = "great")
        every {
            voiceAnswerGradingRepository.transcribeAndGradeSpokenAnswer(
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
            voiceAnswerGradingRepository.transcribeAndGradeSpokenAnswer(
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
            voiceAnswerGradingRepository.transcribeAndGradeSpokenAnswer(any(), any(), any(), any())
        } returns flow { throw error }

        useCase(params).test {
            awaitError() shouldBe error
        }
    }
}
