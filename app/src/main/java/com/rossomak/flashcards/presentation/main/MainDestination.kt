package com.rossomak.flashcards.presentation.main

import com.rossomak.flashcards.ui.navigation.NavigationDestination

sealed interface MainDestination : NavigationDestination {
    data object Login : MainDestination
}
