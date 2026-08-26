package com.rossomak.flashcards.presentation.splash

import com.rossomak.flashcards.core.ui.navigation.NavigationEvent

sealed interface SplashDestination : NavigationEvent {
    data object Main : SplashDestination
    data object Onboarding : SplashDestination
    data object Login : SplashDestination
}
