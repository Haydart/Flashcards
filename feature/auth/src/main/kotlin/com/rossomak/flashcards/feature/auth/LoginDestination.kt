package com.rossomak.flashcards.feature.auth

import com.rossomak.flashcards.core.ui.navigation.NavigationEvent

sealed interface LoginDestination : NavigationEvent {
    data object Main : LoginDestination
}
