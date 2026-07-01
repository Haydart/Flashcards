package com.rossomak.flashcards.core.domain.model

import java.time.Instant

data class CurationRequest(
    val cardId: String,
    val subcategoryId: String,
    val actions: Map<CurationAction, Instant>,
)
