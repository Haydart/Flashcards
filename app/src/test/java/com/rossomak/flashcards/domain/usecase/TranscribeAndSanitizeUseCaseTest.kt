package com.rossomak.flashcards.domain.usecase

import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import com.rossomak.flashcards.core.domain.usecase.TranscribeAndSanitizeUseCase
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TranscribeAndSanitizeUseCaseTest {

    private val voiceAnswerGradingRepository: VoiceAnswerGradingRepository = mockk()
    private val useCase = TranscribeAndSanitizeUseCase(voiceAnswerGradingRepository)

    @Test
    fun `forwards wav bytes to repository and returns the sanitized transcript`() = runTest {
        val wavBytes = byteArrayOf(1, 2, 3)
        val transcript = "a mechanism keeping the cpu awake"
        coEvery { voiceAnswerGradingRepository.transcribeAndSanitize(wavBytes) } returns Result.success(transcript)

        val result = useCase(wavBytes)

        result.getOrNull() shouldBe transcript
        coVerify(exactly = 1) { voiceAnswerGradingRepository.transcribeAndSanitize(wavBytes) }
    }

    @Test
    fun `forwards failure result unchanged`() = runTest {
        val wavBytes = byteArrayOf(1, 2, 3)
        val error = IllegalStateException("stt down")
        coEvery { voiceAnswerGradingRepository.transcribeAndSanitize(wavBytes) } returns Result.failure(error)

        val result = useCase(wavBytes)

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { voiceAnswerGradingRepository.transcribeAndSanitize(wavBytes) }
    }
}
