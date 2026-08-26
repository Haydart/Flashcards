package com.rossomak.flashcards.feature.settings

import com.rossomak.flashcards.core.ui.navigation.NavigationEvent

sealed interface SettingsDestination : NavigationEvent {
    data object Login : SettingsDestination

    /** Debug-only: replays the onboarding flow from the Settings screen. */
    data object Onboarding : SettingsDestination
}
