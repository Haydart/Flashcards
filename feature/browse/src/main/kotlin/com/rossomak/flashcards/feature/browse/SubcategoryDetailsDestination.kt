package com.rossomak.flashcards.feature.browse

import com.rossomak.flashcards.core.ui.navigation.NavigationEvent

sealed interface SubcategoryDetailsDestination : NavigationEvent {
    data class PreviewStudySession(
        val categoryId: String,
        val categoryName: String,
        val subcategoryId: String,
        val subcategoryName: String
    ) : SubcategoryDetailsDestination
}
