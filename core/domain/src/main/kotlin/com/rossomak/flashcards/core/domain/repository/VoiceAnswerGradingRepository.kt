package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade

/**
 * Boundary to the voice grading backend (Cloud Function proxy → ElevenLabs Scribe + grading
 * LLM). Implementations receive already-obfuscated WAV audio only; obfuscation happens in
 * `core:voice` before any bytes reach this interface.
 */
interface VoiceAnswerGradingRepository {

    /**
     * Full pipeline for one spoken answer: transcribe + sanitize + grade. Returns the resulting
     * [VoiceAnswerGrade] for [cardId] only — no persistence happens here (ADR-0014: no
     * per-card Firestore writes during a session). The caller batches grades into the
     * session-end write once that pipeline exists.
     */
    suspend fun gradeSpokenAnswer(
        cardId: String,
        question: String,
        expectedAnswer: String,
        obfuscatedAnswerWav: ByteArray
    ): Result<VoiceAnswerGrade>

    /** STT step in isolation (debug screen): obfuscated WAV in, raw transcript out. */
    suspend fun transcribe(obfuscatedAnswerWav: ByteArray): Result<String>

    /** Sanitize+grade LLM step in isolation (debug screen): typed transcript, no audio. */
    suspend fun sanitizeAndGrade(question: String, expectedAnswer: String, rawTranscript: String): Result<VoiceAnswerGrade>

    /**
     * Asks the backend whether the current user holds a premium entitlement. The check itself
     * is server-side per request; this only surfaces the verdict (used by the debug screen's
     * entitlement test block).
     */
    suspend fun checkEntitlement(): Result<Boolean>
}
