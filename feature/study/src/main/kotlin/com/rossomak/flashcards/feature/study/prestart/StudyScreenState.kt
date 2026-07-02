package com.rossomak.flashcards.feature.study.prestart

import com.rossomak.flashcards.core.domain.model.Category
import com.rossomak.flashcards.core.ui.navigation.NavigationEvent

sealed interface StudyNavigationDestination : NavigationEvent {
    data class CategoryDetails(val categoryId: String, val categoryName: String) : StudyNavigationDestination
}

data class StudyScreenState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val error: String? = null,
)
