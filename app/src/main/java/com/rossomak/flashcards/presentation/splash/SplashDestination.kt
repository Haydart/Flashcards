package com.rossomak.flashcards.presentation.splash

import com.rossomak.flashcards.ui.navigation.NavigationDestination

sealed interface SplashDestination : NavigationDestination {
    data object Main : SplashDestination
    data object Login : SplashDestination
}
