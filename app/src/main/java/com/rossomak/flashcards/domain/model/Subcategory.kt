package com.rossomak.flashcards.domain.model

data class Subcategory(
    val id: String,
    val name: String,
    val categoryId: String,
    val order: Int,
    val cardCount: Int
)