package com.rossomak.flashcards.core.data.model

import com.google.firebase.Timestamp

data class CurationRequestDto(
    val subcategoryId: String = "",
    val actions: Map<String, CurationActionEntryDto> = emptyMap(),
)

data class CurationActionEntryDto(
    val flaggedAt: Timestamp? = null,
)
