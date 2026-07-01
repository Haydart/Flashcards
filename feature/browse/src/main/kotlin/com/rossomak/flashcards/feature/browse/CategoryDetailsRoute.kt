package com.rossomak.flashcards.feature.browse

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDetailsRoute(val categoryId: String, val categoryName: String)
