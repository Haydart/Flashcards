package com.rossomak.flashcards.core.data.network

import com.rossomak.flashcards.core.data.model.SanitizeAndGradeRequestDto
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeVoiceGradingApiTest {

    private val debugSettings = VoicePipelineDebugSettings()
    private val fakeApi = FakeVoiceGradingApi(debugSettings).apply {
        isTransientFailureInjectionEnabled = false
        isLatencySimulationEnabled = false
    }

    private val question = "What does a foreground service require?"
    private val expectedAnswer = "A persistent notification visible to the user"

    @Test
    fun `sanitizeAndGrade strips emails from the transcript`() = runTest {
        val response = fakeApi.sanitizeAndGrade(
            SanitizeAndGradeRequestDto(
                question = question,
                expectedAnswer = expectedAnswer,
                transcript = "send it to someone@example.com please",
            )
        )

        response.sanitizedTranscript shouldNotContain "someone@example.com"
        response.sanitizedTranscript shouldContain "[redacted]"
    }

    @Test
    fun `sanitizeAndGrade strips blurted names and phone numbers`() = runTest {
        val response = fakeApi.sanitizeAndGrade(
            SanitizeAndGradeRequestDto(
                question = question,
                expectedAnswer = expectedAnswer,
                transcript = "my name is John Smith call +48 601 234 567",
            )
        )

        response.sanitizedTranscript shouldNotContain "John"
        response.sanitizedTranscript shouldNotContain "601"
    }

    @Test
    fun `sanitizeAndGrade removes disfluencies and immediate word repeats`() = runTest {
        val response = fakeApi.sanitizeAndGrade(
            SanitizeAndGradeRequestDto(
                question = question,
                expectedAnswer = expectedAnswer,
                transcript = "um a persistent persistent notification uh visible",
            )
        )

        response.sanitizedTranscript.lowercase() shouldNotContain "um"
        response.sanitizedTranscript.lowercase() shouldNotContain "uh"
        response.sanitizedTranscript.lowercase() shouldBe "a persistent notification visible"
    }

    @Test
    fun `sanitizeAndGrade returns a grade within the valid range`() = runTest {
        val response = fakeApi.sanitizeAndGrade(
            SanitizeAndGradeRequestDto(
                question = question,
                expectedAnswer = expectedAnswer,
                transcript = "a persistent notification visible to the user",
            )
        )

        response.gradePercent shouldBeInRange 0..100
        response.feedback.isNotBlank() shouldBe true
    }

    @Test
    fun `gradeVoiceAnswer throws entitlement exception when premium simulation is off`() = runTest {
        debugSettings.setSimulatePremiumEntitlement(false)

        val thrown = runCatching {
            fakeApi.gradeVoiceAnswer("card-1", question, expectedAnswer, ByteArray(64))
        }.exceptionOrNull()

        (thrown is VoiceGradingEntitlementException) shouldBe true
    }

    @Test
    fun `checkEntitlement reflects the simulated premium record`() = runTest {
        debugSettings.setSimulatePremiumEntitlement(false)
        fakeApi.checkEntitlement().isPremium shouldBe false

        debugSettings.setSimulatePremiumEntitlement(true)
        fakeApi.checkEntitlement().isPremium shouldBe true
    }

    @Test
    fun `gradeVoiceAnswer produces a transcript derived from the expected answer`() = runTest {
        val longExpectedAnswer = "a foreground service keeps running with a persistent notification " +
                "shown to the user even when the app itself is no longer visible on screen"

        val response = fakeApi.gradeVoiceAnswer(
            "card-1",
            question,
            longExpectedAnswer,
            ByteArray(64_000),
        )

        response.gradePercent shouldBeInRange 0..100
        response.sanitizedTranscript.isNotBlank() shouldBe true
    }
}
