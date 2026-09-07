package com.rossomak.flashcards.core.domain.model

import java.time.Instant

/**
 * One card's persisted progress entry inside a [SubcategoryProgress]'s packed `cards` map
 * ([ADR-0016](../../../../../../../docs/adr/0016-card-progress-model.md)). [masteredAt] is the most
 * recent time the card reached [FlashcardStudyProgressState.Mastered]; it is retained after a later
 * de-mastery, never cleared, so it only ever changes by being overwritten with a later mastery.
 */
data class CardProgressEntry(
    val state: FlashcardStudyProgressState,
    val firstStudiedAt: Instant,
    val masteredAt: Instant?,
)

/**
 * One User's packed per-Subcategory progress document (ADR-0016):
 * `users/{uid}/progress/{subcategoryId}`. Only cards the User has actually studied appear in
 * [cards]. [categoryId] rides along denormalized, so a document identifies its own scope without a
 * further lookup.
 */
data class SubcategoryProgress(
    val subcategoryId: String,
    val categoryId: String,
    val cards: Map<String, CardProgressEntry>,
)

/**
 * What a session commit writes for one card, decided by [com.rossomak.flashcards.core.domain.usecase.CommitStudySessionUseCase]
 * from a read of the card's prior [CardProgressEntry] (or its absence). Carries no [Instant] of its
 * own — [stampFirstStudied] and [stampMastered] are intent flags the data layer turns into
 * `FieldValue.serverTimestamp()`, since only the server, not this pure-Kotlin use case, may decide
 * what "now" means for a persisted record.
 */
data class CardProgressUpdate(
    val state: FlashcardStudyProgressState,
    val stampFirstStudied: Boolean,
    val stampMastered: Boolean,
)

/**
 * The subset of a [SubcategoryProgress] a single session commit actually changes — only the cards
 * this session touched, never the whole map. Writing this as a nested-key merge is what leaves every
 * card this session did not touch byte-for-byte intact (ADR-0016's single most dangerous mistake to
 * get wrong). [categoryId] rides along so a brand-new progress document can be created with it in
 * the same write; re-writing the same value onto an existing document is harmless.
 */
data class SubcategoryProgressWrite(
    val subcategoryId: String,
    val categoryId: String,
    val cards: Map<String, CardProgressUpdate>,
)
