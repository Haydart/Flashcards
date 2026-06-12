package com.rossomak.flashcards.presentation.study

import com.rossomak.flashcards.ui.navigation.NavigationDestination

sealed interface StudyDestination : NavigationDestination {
    data class CategoryDetails(val categoryId: String) : StudyDestination
    data class SubcategoryDetails(val categoryId: String, val subcategoryId: String) : StudyDestination
}
