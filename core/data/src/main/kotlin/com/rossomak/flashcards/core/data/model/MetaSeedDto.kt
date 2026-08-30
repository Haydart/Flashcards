package com.rossomak.flashcards.core.data.model

/** Maps `meta/seed`, the cache-generation freshness signal (ADR-0039). */
data class MetaSeedDto(
    val value: Int = 0,
)
