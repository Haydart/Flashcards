package com.rossomak.flashcards.core.data.repository

import app.cash.turbine.test
import com.rossomak.flashcards.core.data.model.EntitlementDto
import com.rossomak.flashcards.core.data.model.SanitizeAndGradeRequestDto
import com.rossomak.flashcards.core.data.model.TranscriptionDto
import com.rossomak.flashcards.core.data.model.VoiceAnswerGradeDto
import com.rossomak.flashcards.core.data.model.VoiceGradingStreamEventDto
import com.rossomak.flashcards.core.data.network.VoiceGradingApi
import com.rossomak.flashcards.core.data.network.VoiceGradingEntitlementException
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGradingEvent
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultVoiceAnswerGradingRepositoryTest {

    private val voiceGradingApi: VoiceGradingApi = mockk()

    private val cardId = "card-1"
    private val question = "What is a foreground service?"
    private val expectedAnswer = "A service with a persistent notification"
    private val wavBytes = byteArrayOf(1, 2, 3)
    private val sanitizedTranscript = "A service with a notification"
    private val expectedGrade = VoiceAnswerGrade(
        sanitizedTranscript = sanitizedTranscript,
        gradePercent = 80,
        feedback = "Mostly right",
    )

    private fun createRepository(): DefaultVoiceAnswerGradingRepository =
        DefaultVoiceAnswerGradingRepository(voiceGradingApi)

    private fun successfulStream() = flow {
        emit(VoiceGradingStreamEventDto.TranscriptChunk(sanitizedTranscript))
        emit(VoiceGradingStreamEventDto.Graded(expectedGrade.gradePercent, expectedGrade.feedback))
    }

    @Test
    fun `gradeSpokenAnswer streams transcript then grade`() = runTest {
        every { voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes) } returns successfulStream()

        createRepository().gradeSpokenAnswer(cardId, question, expectedAnswer, wavBytes).test {
            awaitItem() shouldBe VoiceAnswerGradingEvent.TranscriptReady(sanitizedTranscript)
            awaitItem() shouldBe VoiceAnswerGradingEvent.Graded(expectedGrade)
            awaitComplete()
        }
    }

    @Test
    fun `gradeSpokenAnswer retries the whole call on transient io failures before succeeding`() = runTest {
        every {
            voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes)
        } returns flow<VoiceGradingStreamEventDto> {
            throw IOException("flaky")
        } andThen flow<VoiceGradingStreamEventDto> {
            throw IOException("flaky again")
        } andThen successfulStream()

        createRepository().gradeSpokenAnswer(cardId, question, expectedAnswer, wavBytes).test {
            awaitItem() shouldBe VoiceAnswerGradingEvent.TranscriptReady(sanitizedTranscript)
            awaitItem() shouldBe VoiceAnswerGradingEvent.Graded(expectedGrade)
            awaitComplete()
        }
    }

    @Test
    fun `gradeSpokenAnswer gives up after exhausting retries and surfaces the failure`() = runTest {
        val error = IOException("network down")
        every {
            voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes)
        } returns flow<VoiceGradingStreamEventDto> { throw error }

        createRepository().gradeSpokenAnswer(cardId, question, expectedAnswer, wavBytes).test {
            awaitError() shouldBe error
        }
    }

    @Test
    fun `gradeSpokenAnswer surfaces entitlement rejection without retrying`() = runTest {
        val error = VoiceGradingEntitlementException()
        every {
            voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes)
        } returns flow<VoiceGradingStreamEventDto> { throw error }

        createRepository().gradeSpokenAnswer(cardId, question, expectedAnswer, wavBytes).test {
            awaitError() shouldBe error
        }
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
        val gradeDto = VoiceAnswerGradeDto(
            sanitizedTranscript = expectedGrade.sanitizedTranscript,
            gradePercent = expectedGrade.gradePercent,
            feedback = expectedGrade.feedback,
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
