package com.rossomak.flashcards.domain.usecase

import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import com.rossomak.flashcards.core.domain.usecase.SanitizeAndGradeTranscriptUseCase
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SanitizeAndGradeTranscriptUseCaseTest {

    private val voiceAnswerGradingRepository: VoiceAnswerGradingRepository = mockk()
    private val useCase = SanitizeAndGradeTranscriptUseCase(voiceAnswerGradingRepository)

    private val params = SanitizeAndGradeTranscriptUseCase.Params(
        question = "What is a wake lock?",
        expectedAnswer = "A mechanism keeping the CPU awake",
        rawTranscript = "um a mechanism keeping the cpu awake",
    )

    @Test
    fun `forwards params to repository and returns the grade`() = runTest {
        val grade = VoiceAnswerGrade(sanitizedTranscript = "clean", gradePercent = 75, feedback = "solid")
        coEvery {
            voiceAnswerGradingRepository.sanitizeAndGrade(
                params.question,
                params.expectedAnswer,
                params.rawTranscript,
            )
        } returns Result.success(grade)

        val result = useCase(params)

        result.getOrNull() shouldBe grade
        coVerify(exactly = 1) {
            voiceAnswerGradingRepository.sanitizeAndGrade(
                params.question,
                params.expectedAnswer,
                params.rawTranscript,
            )
        }
    }

    @Test
    fun `forwards failure result unchanged`() = runTest {
        val error = IllegalStateException("backend down")
        coEvery {
            voiceAnswerGradingRepository.sanitizeAndGrade(any(), any(), any())
        } returns Result.failure(error)

        val result = useCase(params)

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { voiceAnswerGradingRepository.sanitizeAndGrade(any(), any(), any()) }
    }
}
