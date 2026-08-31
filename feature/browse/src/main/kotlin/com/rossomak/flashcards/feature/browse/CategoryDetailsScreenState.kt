package com.rossomak.flashcards.feature.browse

import com.rossomak.flashcards.core.domain.model.Subcategory

/**
 * @param selectedSubcategoryIds **one nullable field, not a boolean plus a set.** `null` means
 * default mode; a set (possibly empty) means Selection Mode, so "not in selection mode but three
 * topics selected" is unrepresentable — the same discipline that makes
 * [SubcategoryDetailsContentState] sealed rather than a loading flag plus a nullable error plus a
 * list.
 * @param isFavorite deliberately fake — see [CategoryDetailsViewModel.onFavoriteToggle].
 */
data class CategoryDetailsScreenState(
    val categoryId: String = "",
    val categoryName: String = "",
    val isLoading: Boolean = false,
    val subcategories: List<Subcategory> = emptyList(),
    val error: String? = null,
    val selectedSubcategoryIds: Set<String>? = null,
    val isFavorite: Boolean = false,
) {

    val isSelectionMode: Boolean
        get() = selectedSubcategoryIds != null

    val selectedCount: Int
        get() = selectedSubcategoryIds?.size ?: 0

    /** Sum of [Subcategory.cardCount] across the selected Subcategories — the CTA's session size. */
    val selectedCardCount: Int
        get() {
            val selectedIds = selectedSubcategoryIds ?: return 0
            return subcategories.filter { it.id in selectedIds }.sumOf { it.cardCount }
        }

    /** False for an empty Category, so select-all never claims everything is selected when nothing exists. */
    val isAllSelected: Boolean
        get() = subcategories.isNotEmpty() && selectedCount == subcategories.size
}
