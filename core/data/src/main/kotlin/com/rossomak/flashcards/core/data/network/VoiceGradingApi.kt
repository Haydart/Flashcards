package com.rossomak.flashcards.core.data.network

import com.rossomak.flashcards.core.data.model.EntitlementDto
import com.rossomak.flashcards.core.data.model.SanitizeAndGradeRequestDto
import com.rossomak.flashcards.core.data.model.TranscriptionDto
import com.rossomak.flashcards.core.data.model.VoiceAnswerGradeDto

/**
 * Client-side contract of the Firebase Cloud Function proxy. The proxy verifies the Firebase
 * ID token and premium entitlement server-side, forwards the WAV to ElevenLabs Scribe, runs
 * the combined sanitize+grade LLM call, and never persists audio.
 *
 * [RetrofitVoiceGradingApi] talks to the real deployment; [FakeVoiceGradingApi] simulates it
 * with realistic behavior when credentials/infra are unavailable. [VoiceGradingApiRouter]
 * picks per stage. Swapping fake → real is a DI/config change only.
 */
interface VoiceGradingApi {

    /** Full pipeline call: obfuscated WAV in, `{sanitized_transcript, grade, feedback}` out. */
    suspend fun gradeVoiceAnswer(
        cardId: String,
        question: String,
        expectedAnswer: String,
        wavBytes: ByteArray,
    ): VoiceAnswerGradeDto

    /** STT step only. */
    suspend fun transcribe(wavBytes: ByteArray): TranscriptionDto

    /** Sanitize+grade LLM step only, no audio involved. */
    suspend fun sanitizeAndGrade(request: SanitizeAndGradeRequestDto): VoiceAnswerGradeDto

    /** Server-side premium entitlement verdict for the calling user. */
    suspend fun checkEntitlement(): EntitlementDto
}
