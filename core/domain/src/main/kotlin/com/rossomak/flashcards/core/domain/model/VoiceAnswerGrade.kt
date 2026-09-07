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

/**
 * Maps [VoiceAnswerGrade.gradePercent] onto the same three-valued [FlashcardRating] a manual tap
 * produces — the fixed grade-band mapping (Failed < 40, Partial 40-79, Correct >= 80) from
 * [ADR-0031](../../../../../../../docs/adr/0031-voice-answering-shared-tts-engine-silence-timeout-grade-bands.md),
 * unified onto the Rating pipeline by
 * [ADR-0026](../../../../../../../docs/adr/0026-voice-grade-unifies-with-rating-reveal-tied-to-speech-end.md).
 * The one place this mapping is defined, so the voice pipeline and the Rating write path it feeds
 * (ticket 04 of the Rated session state machine sequence) can't drift apart.
 */
fun VoiceAnswerGrade.toFlashcardRating(): FlashcardRating = when {
    gradePercent >= VOICE_GRADE_CORRECT_THRESHOLD -> FlashcardRating.Correct
    gradePercent >= VOICE_GRADE_PARTIAL_THRESHOLD -> FlashcardRating.PartiallyCorrect
    else -> FlashcardRating.Failed
}

private const val VOICE_GRADE_PARTIAL_THRESHOLD = 40
private const val VOICE_GRADE_CORRECT_THRESHOLD = 80
