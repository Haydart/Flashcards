package com.rossomak.flashcards.domain.usecase

import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import com.rossomak.flashcards.core.domain.usecase.GradeSpokenAnswerUseCase
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    fun `forwards params to repository and returns the grade`() = runTest {
        val grade = VoiceAnswerGrade(sanitizedTranscript = "clean", gradePercent = 90, feedback = "great")
        coEvery {
            voiceAnswerGradingRepository.gradeSpokenAnswer(
                params.cardId,
                params.question,
                params.expectedAnswer,
                params.obfuscatedAnswerWav,
            )
        } returns Result.success(grade)

        val result = useCase(params)

        result.getOrNull() shouldBe grade
        coVerify(exactly = 1) {
            voiceAnswerGradingRepository.gradeSpokenAnswer(
                params.cardId,
                params.question,
                params.expectedAnswer,
                params.obfuscatedAnswerWav,
            )
        }
    }

    @Test
    fun `forwards failure result unchanged`() = runTest {
        val error = IllegalStateException("backend down")
        coEvery {
            voiceAnswerGradingRepository.gradeSpokenAnswer(any(), any(), any(), any())
        } returns Result.failure(error)

        val result = useCase(params)

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) {
            voiceAnswerGradingRepository.gradeSpokenAnswer(any(), any(), any(), any())
        }
    }
}
