package com.rossomak.flashcards.core.domain.model

/**
 * A single Attempt's grade within a Rated Study Session — Failed, Partial or Correct for one
 * answer, not one card's overall outcome. A card's session-final outcome is [FlashcardTerminalRating],
 * resolved from the best [FlashcardAttemptRating] across all its Attempts; collapsing the two is
 * exactly the confusion [ADR-0044](../../../../../../../docs/adr/0044-three-valued-terminal-state.md)
 * exists to prevent.
 */
enum class FlashcardAttemptRating {
    Failed,
    PartiallyCorrect,
    Correct,
}
