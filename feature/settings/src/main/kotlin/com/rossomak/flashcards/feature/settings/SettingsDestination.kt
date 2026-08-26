package com.rossomak.flashcards.feature.settings

import com.rossomak.flashcards.core.ui.navigation.NavigationEvent

sealed interface SettingsDestination : NavigationEvent {
    data object Login : SettingsDestination
}
