package com.rossomak.flashcards.data.model

data class CategoryDto(
    val id: String = "",
    val name: String = "",
    val order: Int = 0,
    val subcategoryCount: Int = 0,
    val iconUrl: String? = null
)
