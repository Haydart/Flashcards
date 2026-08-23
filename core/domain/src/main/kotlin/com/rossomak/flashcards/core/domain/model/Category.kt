package com.rossomak.flashcards.core.domain.model

/**
 * [featuredSubcategoryNames] holds up to five of this category's Subcategory display names, ranked by
 * card volume descending — the same prominence ranking behind each `Subcategory.order`. It is
 * denormalized onto the Category document so the Browse screen's subcategory-summary chip line
 * can be built from a single `categories` read, without touching the `subcategories` collection.
 * Empty for a category that has no subcategories yet. See docs/design/category-search.md.
 */
data class Category(
    val id: String,
    val name: String,
    val order: Int,
    val subcategoryCount: Int,
    val iconSvg: String?,
    val color: String?,
    val featuredSubcategoryNames: List<String>,
)
