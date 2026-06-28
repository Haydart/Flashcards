package com.rossomak.flashcards.presentation.categorydetails

import com.rossomak.flashcards.core.domain.model.Subcategory

data class CategoryDetailsScreenState(
    val categoryId: String = "",
    val categoryName: String = "",
    val isLoading: Boolean = false,
    val subcategories: List<Subcategory> = emptyList(),
    val error: String? = null,
)
