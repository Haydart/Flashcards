package com.rossomak.flashcards.presentation.settings

import com.rossomak.flashcards.ui.navigation.NavigationDestination

sealed interface SettingsDestination : NavigationDestination {
    data object Login : SettingsDestination
}

