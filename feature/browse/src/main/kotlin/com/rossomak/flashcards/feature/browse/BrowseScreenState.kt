package com.rossomak.flashcards.feature.browse

import com.rossomak.flashcards.core.domain.model.Category
import com.rossomak.flashcards.core.ui.navigation.NavigationEvent

sealed interface BrowseNavigationDestination : NavigationEvent {
    data class CategoryDetails(val categoryId: String, val categoryName: String) : BrowseNavigationDestination
}

data class BrowseScreenState(val isLoading: Boolean = false, val categories: List<Category> = emptyList(), val error: String? = null)
