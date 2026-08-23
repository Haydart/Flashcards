package com.rossomak.flashcards.core.data.model

/**
 * [nameLower] backs the case-insensitive prefix-range search query and has no UI consumer, so it
 * deliberately stops here — the mapper drops it and the domain `Subcategory` never carries it.
 */
data class SubcategoryDto(
    val id: String = "",
    val name: String = "",
    val nameLower: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val order: Int = 0,
    val cardCount: Int = 0
)
