package com.rossomak.flashcards.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class CardSortOrder {
    DEFAULT,
    EASIEST_FIRST,
    HARDEST_FIRST,
}
