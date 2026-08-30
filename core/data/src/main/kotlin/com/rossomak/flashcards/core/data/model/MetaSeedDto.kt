package com.rossomak.flashcards.core.data.model

/**
 * Maps `meta/seed`, the cache-generation freshness signal (ADR-0039). [value] stays nullable with
 * no default: a doc missing the field must deserialize as `null`, not a fabricated `0`, so
 * [com.rossomak.flashcards.core.data.source.FlashcardRemoteDataSource.getCacheSeed]'s
 * missing-or-empty guard actually fires instead of silently treating a malformed doc as a valid,
 * very-stale seed.
 */
data class MetaSeedDto(
    val value: Int? = null,
)
