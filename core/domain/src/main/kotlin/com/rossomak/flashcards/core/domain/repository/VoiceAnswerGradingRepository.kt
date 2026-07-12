package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.VoiceAnswerGradingEvent
import kotlinx.coroutines.flow.Flow

/**
 * Boundary to the voice grading backend (Cloud Function proxy → ElevenLabs Scribe + grading
 * LLM). Implementations receive already-obfuscated WAV audio only; obfuscation happens in
 * `core:voice` before any bytes reach this interface.
 */
interface VoiceAnswerGradingRepository {

    /**
     * Full pipeline for one spoken answer, streamed over a single connection (ADR-0028):
     * [VoiceAnswerGradingEvent.TranscriptReady] as soon as STT + sanitize finish, then
     * [VoiceAnswerGradingEvent.Graded] once grading finishes. No persistence happens here
     * (ADR-0014: no per-card Firestore writes during a session) — the caller batches grades
     * into the session-end write once that pipeline exists. Failures propagate as flow
     * exceptions (collect with `.catch()`), not a wrapped [Result], per this project's Flow
     * error convention.
     */
    fun transcribeAndGradeSpokenAnswer(
        cardId: String,
        question: String,
        expectedAnswer: String,
        obfuscatedAnswerWav: ByteArray,
    ): Flow<VoiceAnswerGradingEvent>

    /**
     * Transcribe + sanitize in isolation (debug screen): obfuscated WAV in, sanitized transcript
     * out. Rides the same backend call as [transcribeAndGradeSpokenAnswer] with no question/answer,
     * so the grade step is skipped server-side (ADR-0029 §3–4).
     */
    suspend fun transcribeAndSanitize(obfuscatedAnswerWav: ByteArray): Result<String>

    /**
     * Asks the backend whether the current user holds a premium entitlement. The check itself
     * is server-side per request; this only surfaces the verdict (used by the debug screen's
     * entitlement test block).
     */
    suspend fun checkEntitlement(): Result<Boolean>
}
