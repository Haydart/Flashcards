package com.rossomak.flashcards.domain.model

import java.time.Instant

data class CurationRequest(
    val cardId: String,
    val subcategoryId: String,
    val actions: Map<CurationAction, Instant>,
)
