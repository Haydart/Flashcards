package com.rossomak.flashcards.core.domain.model

/**
 * Persisted defaults for study session parameters — the values a session opens with, editable
 * from both Settings and the Preview Study Session Screen's `keepAsDefault` checkboxes. Named to
 * state the boundary explicitly: preferences that regard the session study parameters, as
 * opposed to app/device-scoped concerns on [UserPreferences].
 *
 * Never a session's *actual* config — the Preview Study Session Screen is the only place a
 * concrete session's Study Mode (and every other value here) is chosen (ADR-0030); these are just
 * the values it opens with.
 */
data class StudySessionPreferences(
    val defaultStudyMode: StudyMode = StudyMode.Rated,
    val voiceAnsweringEnabled: Boolean = false,
    val ratedAttempts: Int = StudySessionConfig.DEFAULT_RATED_ATTEMPTS,
    val readAloudEnabled: Boolean = false,
    val sessionLength: Int = StudySessionConfig.DEFAULT_LENGTH,
    val sortOrder: FlashcardSortOrder = FlashcardSortOrder.Default,
    val voiceSettings: VoiceSettings = VoiceSettings(),
    val subcategoryCountRange: IntRange = StudySessionConfig.DEFAULT_SUBCATEGORY_COUNT_RANGE,
)
