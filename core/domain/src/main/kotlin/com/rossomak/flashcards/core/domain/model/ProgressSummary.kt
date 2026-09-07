package com.rossomak.flashcards.core.domain.model

/**
 * One Subcategory's rollup inside a User's [ProgressSummary]
 * ([ADR-0016](../../../../../../../docs/adr/0016-card-progress-model.md)): how many of its cards the
 * User has ever studied, and how many they currently have Mastered. Neither is the ring
 * denominator — that is `Subcategory.cardCount`, read from the taxonomy instead of stored here.
 */
data class SubcategoryProgressSummary(
    val masteredCount: Int,
    val studiedCount: Int,
)

/**
 * The User's per-Subcategory progress-summary singleton (ADR-0016):
 * `users/{uid}/state/progressSummary`. One document answers every ring on Category Details and the
 * Home screen's progress displays (spec 06), whatever the Category — the alternative, reading each
 * Subcategory's packed [SubcategoryProgress], would cost one read per topic instead of one per
 * screen.
 *
 * A Subcategory absent from [subcategories] has never been studied; a `null` [ProgressSummary] itself
 * means the User has never finished a session at all. Both render as an empty ring — spec 06's
 * concern, not this type's.
 */
data class ProgressSummary(
    val subcategories: Map<String, SubcategoryProgressSummary>,
)

/**
 * One Subcategory's net change to the [ProgressSummary], decided by
 * [com.rossomak.flashcards.core.domain.usecase.CommitStudySessionUseCase] from the same walk over
 * `cardResults` that produces its [CardProgressUpdate]s. [masteredDelta] is `+1` for a newly mastered
 * card, `-1` for a de-mastered one, and `0` for a defended, Partial or otherwise unchanged card;
 * [studiedDelta] is `+1` for a card with no prior entry and `0` otherwise — it can never be negative,
 * since coverage is monotonic even though mastery is not.
 */
data class SubcategoryProgressSummaryDelta(
    val masteredDelta: Int,
    val studiedDelta: Int,
)

/**
 * What one session commit adds to the User's [ProgressSummary] — nested-key atomic increments, keyed
 * by Subcategory id, applied in the same Firestore batch as the session document and every
 * [SubcategoryProgressWrite] (ADR-0016). Never a separate write, never a read-then-set: the summary
 * must not be able to drift from the progress it summarises within a single commit.
 *
 * A Subcategory whose deltas are both zero never appears in [subcategoryDeltas] — there is nothing to
 * write for it.
 */
data class ProgressSummaryWrite(
    val subcategoryDeltas: Map<String, SubcategoryProgressSummaryDelta>,
)
