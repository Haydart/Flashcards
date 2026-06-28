package com.rossomak.flashcards.core.data.model

data class CategoryDto(
    val id: String = "",
    val name: String = "",
    val order: Int = 0,
    val subcategoryCount: Int = 0,
    val iconUrl: String? = null
)
