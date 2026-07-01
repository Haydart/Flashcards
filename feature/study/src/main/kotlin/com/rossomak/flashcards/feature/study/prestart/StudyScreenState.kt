package com.rossomak.flashcards.feature.study.prestart

import com.rossomak.flashcards.core.domain.model.Category

sealed interface StudyNavigationDestination {
    data class CategoryDetails(val categoryId: String, val categoryName: String) : StudyNavigationDestination
}

data class StudyScreenState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val error: String? = null,
    val navigationDestination: StudyNavigationDestination? = null,
)
