package com.rossomak.flashcards.core.domain.model

/**
 * One writable [UserPreferences] field per case, so a screen writes exactly the preference it
 * changed instead of a read-modify-write of the whole object.
 */
sealed interface UserPreference {
    data class DailyGoalMinutes(val value: Int) : UserPreference

    data class HasSeenOnboarding(val value: Boolean) : UserPreference

    data class VoiceAnswerConsent(val value: Boolean) : UserPreference
}
