package com.rossomak.flashcards.presentation.home

import com.rossomak.flashcards.ui.navigation.NavigationDestination

sealed interface HomeDestination : NavigationDestination {
    data class CategoryDetails(val categoryId: String, val categoryName: String) : HomeDestination
}
