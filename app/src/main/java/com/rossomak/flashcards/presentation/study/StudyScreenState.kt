package com.rossomak.flashcards.presentation.study

import com.rossomak.flashcards.core.domain.model.Category

data class StudyScreenState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val error: String? = null,
    val navigationDestination: StudyDestination? = null,
)
