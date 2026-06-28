package com.rossomak.flashcards.feature.auth

sealed interface LoginDestination {
    data object Main : LoginDestination
}
