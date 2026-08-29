package com.rossomak.flashcards.core.domain.model

/**
 * Applies a [FlashcardSortOrder] to an already-chosen set of Flashcards.
 *
 * Shared by browsing and by a Study Session so the three orders cannot drift apart, but kept out of
 * the filtering step because the two callers apply it at different points: browsing filters then
 * orders the whole matching set, while a Session filters, *draws*, and only then orders. Ordering
 * the pool before the draw would hand back the same easiest — or hardest — cards every session.
 *
 * [FlashcardSortOrder.Default] returns the receiver untouched, which is what makes it mean
 * different things in the two callers: source order when browsing, draw order inside a Session.
 */
fun List<Flashcard>.orderedBy(sortOrder: FlashcardSortOrder): List<Flashcard> = when (sortOrder) {
    FlashcardSortOrder.Default -> this
    FlashcardSortOrder.EasiestFirst -> sortedBy { it.difficulty }
    FlashcardSortOrder.HardestFirst -> sortedByDescending { it.difficulty }
}
