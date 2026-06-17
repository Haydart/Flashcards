package com.rossomak.flashcards.presentation.subcategorydetails

import com.rossomak.flashcards.domain.model.Flashcard

data class SubcategoryDetailsScreenState(
    val categoryName: String = "",
    val subcategoryName: String = "",
    val isLoading: Boolean = false,
    val flashcards: List<Flashcard> = emptyList(),
    val error: String? = null,
    val studySessionNavigation: StudySessionNavigation? = null,
)

data class StudySessionNavigation(val subcategoryId: String, val subcategoryName: String)