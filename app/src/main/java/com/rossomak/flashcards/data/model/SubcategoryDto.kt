package com.rossomak.flashcards.data.model

data class SubcategoryDto(
    val id: String = "",
    val name: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val order: Int = 0,
    val cardCount: Int = 0
)
