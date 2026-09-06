package com.rossomak.flashcards.core.domain.model

import kotlinx.serialization.Serializable

/**
 * A Flashcard's progress outcome as recorded in a [SessionResult]'s ledger — four-valued, unlike
 * the Rated state machine's three-valued [TerminalState]. A Rated card always resolves to Mastered,
 * Partial or Failed and can never be merely Seen; a Fast card has no rating step at all and can only
 * ever produce Seen. Widening [TerminalState] to four values, or narrowing this type to three, would
 * let one type express a state its mode can never actually produce
 * ([ADR-0016](../../../../../../../docs/adr/0016-card-progress-model.md),
 * [ADR-0044](../../../../../../../docs/adr/0044-three-valued-terminal-state.md)).
 *
 * [toFlashcardProgressState] is the one mapping between the two types — a Rated ledger entry is
 * never built from anything else.
 *
 * `@Serializable` so the Session Summary route can carry a ledger entry's outcome directly as a
 * flattened list element (`androidx.navigation` typesafe routes only derive a `NavType` for
 * primitives, enums and lists of those) — the same reason [StudyMode] carries the annotation.
 */
@Serializable
enum class FlashcardProgressState {
    /** Fast mode's only possible outcome: the card's answer was shown, nothing more is known. */
    Seen,

    /** Rated, mapped from [TerminalState.Failed]. */
    Failed,

    /** Rated, mapped from [TerminalState.Partial]. */
    Partial,

    /** Rated, mapped from [TerminalState.Mastered]. */
    Mastered,
}

/** Widens a Rated card's resolved [TerminalState] into the ledger's four-valued [FlashcardProgressState]. */
fun TerminalState.toFlashcardProgressState(): FlashcardProgressState = when (this) {
    TerminalState.Mastered -> FlashcardProgressState.Mastered
    TerminalState.Partial -> FlashcardProgressState.Partial
    TerminalState.Failed -> FlashcardProgressState.Failed
}
