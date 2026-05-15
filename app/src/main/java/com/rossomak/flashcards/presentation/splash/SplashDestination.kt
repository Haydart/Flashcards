package com.rossomak.flashcards.presentation.splash

sealed interface SplashDestination {
    data object Main : SplashDestination
    data object Login : SplashDestination
}
