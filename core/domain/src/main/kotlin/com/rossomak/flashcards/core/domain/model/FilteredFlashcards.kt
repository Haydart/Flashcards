package com.rossomak.flashcards.core.domain.model

/**
 * What one filtering pass over a Flashcard pool yields.
 *
 * @param cards the Flashcards matching the filters, in the pool's own order — ordering is a
 * separate step ([orderedBy]), applied at a different point by each caller.
 * @param poolTags the tag vocabulary of the **whole pool**, never of [cards]. A filter picker offers
 * these as its options, and deriving them from the matches would make a tag disappear from the
 * picker precisely because the user filtered it out — leaving no way to undo the selection.
 * @param totalCount the unfiltered pool size, so a caller can say "4 of 80" without keeping its own
 * copy of the pool alongside this.
 */
data class FilteredFlashcards(
    val cards: List<Flashcard>,
    val poolTags: List<String>,
    val totalCount: Int,
)
