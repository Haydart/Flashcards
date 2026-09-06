package com.rossomak.flashcards.core.domain.model

/**
 * A Flashcard's outcome once a Rated Study Session's [RatedSessionState] has finished asking it —
 * distinct from [FlashcardRating], which is one answer's grade rather than one card's outcome.
 * Collapsing the two is exactly the confusion
 * [ADR-0044](../../../../../../../docs/adr/0044-three-valued-terminal-state.md) exists to prevent.
 */
enum class TerminalState {
    /** The card was rated Correct at least once. */
    Mastered,

    /** The card was never rated Correct but was rated Partial at least once. */
    Partial,

    /** The card was only ever rated Failed. */
    Failed,
}
