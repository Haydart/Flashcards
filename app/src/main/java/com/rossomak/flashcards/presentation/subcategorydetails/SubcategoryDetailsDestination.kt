package com.rossomak.flashcards.presentation.subcategorydetails

import com.rossomak.flashcards.ui.navigation.NavigationDestination

sealed interface SubcategoryDetailsDestination : NavigationDestination {
    data class StudySession(val subcategoryId: String, val subcategoryName: String) : SubcategoryDetailsDestination
}
