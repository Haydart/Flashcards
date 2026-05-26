package com.rossomak.flashcards.presentation.startup

sealed interface AppStartupState {
    data object Loading : AppStartupState
    data class Ready(val authenticated: Boolean) : AppStartupState
}
