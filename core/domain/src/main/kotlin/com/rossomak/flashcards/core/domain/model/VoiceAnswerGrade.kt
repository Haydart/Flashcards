package com.rossomak.flashcards.core.domain.model

/**
 * Result of grading a spoken answer: the PII-stripped, disfluency-normalized transcript plus
 * the LLM's completeness grade and feedback. This is the only artifact of a spoken answer that
 * is ever persisted — never audio, never an un-sanitized transcript.
 */
data class VoiceAnswerGrade(
    val sanitizedTranscript: String,
    val gradePercent: Int,
    val feedback: String,
)
