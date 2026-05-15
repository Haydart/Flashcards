package com.rossomak.flashcards.presentation.login

import com.rossomak.flashcards.ui.navigation.NavigationDestination

sealed interface LoginDestination : NavigationDestination {
    data object Main : LoginDestination
}
