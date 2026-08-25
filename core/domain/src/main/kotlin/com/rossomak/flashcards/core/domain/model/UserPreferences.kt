package com.rossomak.flashcards.core.domain.model

/**
 * Device-scoped user preferences held in local storage, not Firestore.
 *
 * [hasSeenOnboarding] is deliberately device-scoped rather than uid-scoped: signing a second
 * account into the same device skips onboarding, and the same account on a new device sees it
 * again. Accepted trade-off — see docs/design/onboarding-flow.md.
 */
data class UserPreferences(
    val hasSeenOnboarding: Boolean = false,
    val studyPreferences: StudyPreferences = StudyPreferences(),
)
