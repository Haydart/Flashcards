package com.rossomak.flashcards.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response shape of the Cloud Function's combined sanitize+grade LLM call — the single JSON
 * contract the design doc locks in: `{sanitized_transcript, grade, feedback}`.
 */
@Serializable
data class VoiceAnswerGradeDto(
    @SerialName("sanitized_transcript") val sanitizedTranscript: String,
    @SerialName("grade") val gradePercent: Int,
    @SerialName("feedback") val feedback: String,
)

/** Response of the transcription-only endpoint (debug screen's isolated STT test). */
@Serializable
data class TranscriptionDto(
    @SerialName("transcript") val transcript: String,
)

/** Request body of the sanitize+grade-only endpoint (debug screen, bypasses audio). */
@Serializable
data class SanitizeAndGradeRequestDto(
    @SerialName("question") val question: String,
    @SerialName("expected_answer") val expectedAnswer: String,
    @SerialName("transcript") val transcript: String,
)

/** Response of the entitlement check endpoint. */
@Serializable
data class EntitlementDto(
    @SerialName("is_premium") val isPremium: Boolean,
)

/**
 * Ordered wire events for the streamed `gradeVoiceAnswer` callable (ADR-0028). Not
 * `@Serializable`/kotlinx.serialization-backed like the DTOs above — the Firebase Functions
 * callable SDK decodes `StreamResponse.Message`/`StreamResponse.Result` payloads into raw
 * `Map<String, Any?>` itself, so [RealVoiceGradingApi] builds these by hand from that map.
 */
sealed interface VoiceGradingStreamEventDto {
    data class TranscriptChunk(val sanitizedTranscript: String) : VoiceGradingStreamEventDto
    data class Graded(val gradePercent: Int, val feedback: String) : VoiceGradingStreamEventDto
}
