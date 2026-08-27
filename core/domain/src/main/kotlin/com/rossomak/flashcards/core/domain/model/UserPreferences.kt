package com.rossomak.flashcards.core.domain.model

/**
 * Device-scoped user preferences held in local storage, not Firestore. App/device-scoped
 * concerns only — session defaults live on [StudySessionPreferences] instead.
 *
 * [hasSeenOnboarding] is deliberately device-scoped rather than uid-scoped: signing a second
 * account into the same device skips onboarding, and the same account on a new device sees it
 * again. Accepted trade-off — see docs/design/onboarding-flow.md.
 */
data class UserPreferences(
    val hasSeenOnboarding: Boolean = false,
    val dailyGoalMinutes: Int = DailyGoal.DEFAULT_MINUTES,
)
