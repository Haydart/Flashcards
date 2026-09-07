package com.rossomak.flashcards.core.domain.model

import kotlinx.serialization.Serializable

/**
 * A Flashcard's progress outcome as recorded in a [SessionResult]'s [SessionResult.cardResults] —
 * four-valued, unlike the Rated state machine's three-valued [FlashcardTerminalRating]. A Rated card
 * always resolves to Mastered, Partial or Failed and can never be merely Seen; a Fast card has no
 * rating step at all and can only ever produce Seen. Widening [FlashcardTerminalRating] to four
 * values, or narrowing this type to three, would let one type express a state its mode can never
 * actually produce
 * ([ADR-0016](../../../../../../../docs/adr/0016-card-progress-model.md),
 * [ADR-0044](../../../../../../../docs/adr/0044-three-valued-terminal-state.md)).
 *
 * [toFlashcardStudyProgressState] is the one mapping between the two types — a Rated [FlashcardResult]
 * is never built from anything else.
 *
 * `@Serializable` so the Session Summary route can carry a [FlashcardResult]'s outcome directly as a
 * flattened list element (`androidx.navigation` typesafe routes only derive a `NavType` for
 * primitives, enums and lists of those) — the same reason [StudyMode] carries the annotation.
 */
@Serializable
enum class FlashcardStudyProgressState {
    /** Fast mode's only possible outcome: the card's answer was shown, nothing more is known. */
    Seen,

    /** Rated, mapped from [FlashcardTerminalRating.Failed]. */
    Failed,

    /** Rated, mapped from [FlashcardTerminalRating.Partial]. */
    Partial,

    /** Rated, mapped from [FlashcardTerminalRating.Mastered]. */
    Mastered,
}

/**
 * Widens a Rated card's resolved [FlashcardTerminalRating] into [FlashcardResult]'s four-valued
 * [FlashcardStudyProgressState].
 */
fun FlashcardTerminalRating.toFlashcardStudyProgressState(): FlashcardStudyProgressState = when (this) {
    FlashcardTerminalRating.Mastered -> FlashcardStudyProgressState.Mastered
    FlashcardTerminalRating.Partial -> FlashcardStudyProgressState.Partial
    FlashcardTerminalRating.Failed -> FlashcardStudyProgressState.Failed
}
