package com.rossomak.flashcards.feature.onboarding.model

import androidx.compose.runtime.Immutable

/**
 * One selectable topic on the Favorites step.
 *
 * [id] is a Subcategory id. Nothing reads it yet — favourites are held in screen state and are not
 * persisted in this release — but the field is real so that wiring the write does not change the
 * shape of the screen.
 */
@Immutable
data class FavoriteTopicOption(
    val id: String,
    val name: String,
    val categoryName: String,
)
