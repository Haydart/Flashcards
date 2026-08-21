package com.rossomak.flashcards.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class FlashcardSortOrder {
    Default,
    EasiestFirst,
    HardestFirst,
}
