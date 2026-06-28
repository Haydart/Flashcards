package com.rossomak.flashcards.feature.browse

sealed interface SubcategoryDetailsDestination {
    data class StudySession(val subcategoryId: String, val subcategoryName: String) : SubcategoryDetailsDestination
}
