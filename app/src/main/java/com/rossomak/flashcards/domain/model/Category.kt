package com.rossomak.flashcards.domain.model

data class Category(
    val id: String,
    val name: String,
    val order: Int,
    val subcategoryCount: Int,
    val iconUrl: String?
)