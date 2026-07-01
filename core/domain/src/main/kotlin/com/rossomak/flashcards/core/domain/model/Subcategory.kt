package com.rossomak.flashcards.core.domain.model

data class Subcategory(
    val id: String,
    val name: String,
    val categoryId: String,
    val categoryName: String,
    val order: Int,
    val cardCount: Int
)
