package com.rossomak.flashcards.core.data.network

import com.rossomak.flashcards.core.data.model.EntitlementDto
import com.rossomak.flashcards.core.data.model.SanitizeAndGradeRequestDto
import com.rossomak.flashcards.core.data.model.TranscriptionDto
import com.rossomak.flashcards.core.data.model.VoiceAnswerGradeDto
import com.rossomak.flashcards.core.data.model.VoiceGradingStreamEventDto
import kotlinx.coroutines.flow.Flow

/**
 * Client-side contract of the Firebase Cloud Function proxy. The proxy verifies the Firebase
 * ID token and premium entitlement server-side, forwards the WAV to ElevenLabs Scribe, runs
 * the sanitize then grade LLM calls, and never persists audio.
 *
 * [RealVoiceGradingApi] talks to the real deployment; [FakeVoiceGradingApi] simulates it
 * with realistic behavior when credentials/infra are unavailable. [VoiceGradingApiRouter]
 * picks per stage. Swapping fake → real is a DI/config change only.
 */
interface VoiceGradingApi {

    /**
     * Full pipeline call, streamed over one Firebase Callable connection (ADR-0028): obfuscated
     * WAV in, a [VoiceGradingStreamEventDto.TranscriptChunk] out as soon as STT + sanitize
     * finish, then a [VoiceGradingStreamEventDto.Graded] out once grading finishes.
     */
    fun gradeVoiceAnswer(
        cardId: String,
        question: String,
        expectedAnswer: String,
        wavBytes: ByteArray,
    ): Flow<VoiceGradingStreamEventDto>

    /** STT step only. */
    suspend fun transcribe(wavBytes: ByteArray): TranscriptionDto

    /** Sanitize+grade LLM step only, no audio involved. */
    suspend fun sanitizeAndGrade(request: SanitizeAndGradeRequestDto): VoiceAnswerGradeDto

    /** Server-side premium entitlement verdict for the calling user. */
    suspend fun checkEntitlement(): EntitlementDto
}
