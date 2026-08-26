package com.rossomak.flashcards.core.domain.model

/**
 * Persisted study defaults the user picks during onboarding and can later change in Settings.
 * Neither value starts a session — the Preview Study Session Screen is the only place a concrete
 * session's Study Mode is chosen (ADR-0030); these are just the values it opens with.
 */
data class StudyPreferences(
    val defaultStudyMode: StudyMode = StudyMode.Rated,
    val dailyGoalMinutes: Int = DailyGoal.DEFAULT_MINUTES,
)
