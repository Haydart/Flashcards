package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.model.SanitizeAndGradeRequestDto
import com.rossomak.flashcards.core.data.model.EntitlementDto
import com.rossomak.flashcards.core.data.model.TranscriptionDto
import com.rossomak.flashcards.core.data.model.VoiceAnswerGradeDto
import com.rossomak.flashcards.core.data.network.VoiceGradingApi
import com.rossomak.flashcards.core.data.network.VoiceGradingEntitlementException
import com.rossomak.flashcards.core.data.source.VoiceAnswerRemoteDataSource
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultVoiceAnswerGradingRepositoryTest {

    private val voiceGradingApi: VoiceGradingApi = mockk()
    private val voiceAnswerRemoteDataSource: VoiceAnswerRemoteDataSource = mockk()

    private val cardId = "card-1"
    private val question = "What is a foreground service?"
    private val expectedAnswer = "A service with a persistent notification"
    private val wavBytes = byteArrayOf(1, 2, 3)
    private val gradeDto = VoiceAnswerGradeDto(
        sanitizedTranscript = "A service with a notification",
        gradePercent = 80,
        feedback = "Mostly right",
    )
    private val expectedGrade = VoiceAnswerGrade(
        sanitizedTranscript = gradeDto.sanitizedTranscript,
        gradePercent = gradeDto.gradePercent,
        feedback = gradeDto.feedback,
    )

    private fun createRepository(): DefaultVoiceAnswerGradingRepository =
        DefaultVoiceAnswerGradingRepository(voiceGradingApi, voiceAnswerRemoteDataSource)

    @Test
    fun `gradeSpokenAnswer maps dto to domain and persists the grade`() = runTest {
        coEvery { voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes) } returns gradeDto
        coEvery { voiceAnswerRemoteDataSource.saveVoiceAnswerGrade(cardId, expectedGrade) } just Runs

        val result = createRepository().gradeSpokenAnswer(cardId, question, expectedAnswer, wavBytes)

        result.isSuccess shouldBe true
        result.getOrThrow() shouldBe expectedGrade
        coVerify(exactly = 1) { voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes) }
        coVerify(exactly = 1) { voiceAnswerRemoteDataSource.saveVoiceAnswerGrade(cardId, expectedGrade) }
    }

    @Test
    fun `gradeSpokenAnswer retries transient io failures with backoff before succeeding`() = runTest {
        coEvery {
            voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes)
        } throws IOException("flaky") andThenThrows IOException("flaky again") andThen gradeDto
        coEvery { voiceAnswerRemoteDataSource.saveVoiceAnswerGrade(cardId, expectedGrade) } just Runs

        val result = createRepository().gradeSpokenAnswer(cardId, question, expectedAnswer, wavBytes)

        result.isSuccess shouldBe true
        coVerify(exactly = 3) { voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes) }
        coVerify(exactly = 1) { voiceAnswerRemoteDataSource.saveVoiceAnswerGrade(cardId, expectedGrade) }
    }

    @Test
    fun `gradeSpokenAnswer gives up after exhausting retries and wraps the failure`() = runTest {
        val error = IOException("network down")
        coEvery { voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes) } throws error

        val result = createRepository().gradeSpokenAnswer(cardId, question, expectedAnswer, wavBytes)

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 3) { voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes) }
        coVerify(exactly = 0) { voiceAnswerRemoteDataSource.saveVoiceAnswerGrade(any(), any()) }
    }

    @Test
    fun `gradeSpokenAnswer surfaces entitlement rejection without retrying`() = runTest {
        val error = VoiceGradingEntitlementException()
        coEvery { voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes) } throws error

        val result = createRepository().gradeSpokenAnswer(cardId, question, expectedAnswer, wavBytes)

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes) }
    }

    @Test
    fun `gradeSpokenAnswer rethrows cancellation instead of wrapping it`() = runTest {
        coEvery {
            voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes)
        } throws CancellationException("cancelled")

        val thrown = runCatching {
            createRepository().gradeSpokenAnswer(cardId, question, expectedAnswer, wavBytes)
        }.exceptionOrNull()

        (thrown is CancellationException) shouldBe true
        coVerify(exactly = 1) { voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes) }
    }

    @Test
    fun `transcribe returns the transcript from the api`() = runTest {
        val transcript = "um a service with a notification"
        coEvery { voiceGradingApi.transcribe(wavBytes) } returns TranscriptionDto(transcript)

        val result = createRepository().transcribe(wavBytes)

        result.getOrThrow() shouldBe transcript
        coVerify(exactly = 1) { voiceGradingApi.transcribe(wavBytes) }
    }

    @Test
    fun `transcribe wraps api failure in failure result`() = runTest {
        val error = IOException("boom")
        coEvery { voiceGradingApi.transcribe(wavBytes) } throws error

        val result = createRepository().transcribe(wavBytes)

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { voiceGradingApi.transcribe(wavBytes) }
    }

    @Test
    fun `sanitizeAndGrade forwards the transcript and maps the response`() = runTest {
        val rawTranscript = "um the answer is a service"
        val request = SanitizeAndGradeRequestDto(
            question = question,
            expectedAnswer = expectedAnswer,
            transcript = rawTranscript,
        )
        coEvery { voiceGradingApi.sanitizeAndGrade(request) } returns gradeDto

        val result = createRepository().sanitizeAndGrade(question, expectedAnswer, rawTranscript)

        result.getOrThrow() shouldBe expectedGrade
        coVerify(exactly = 1) { voiceGradingApi.sanitizeAndGrade(request) }
    }

    @Test
    fun `checkEntitlement returns the premium verdict`() = runTest {
        coEvery { voiceGradingApi.checkEntitlement() } returns EntitlementDto(isPremium = true)

        val result = createRepository().checkEntitlement()

        result.getOrThrow() shouldBe true
        coVerify(exactly = 1) { voiceGradingApi.checkEntitlement() }
    }

    @Test
    fun `checkEntitlement wraps api failure in failure result`() = runTest {
        val error = IllegalStateException("boom")
        coEvery { voiceGradingApi.checkEntitlement() } throws error

        val result = createRepository().checkEntitlement()

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { voiceGradingApi.checkEntitlement() }
    }
}
