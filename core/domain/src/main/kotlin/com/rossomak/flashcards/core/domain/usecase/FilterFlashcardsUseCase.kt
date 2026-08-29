package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.FilteredFlashcards
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

/**
 * Narrows a Flashcard pool to the cards matching both filter facets (ADR-0022).
 *
 * The single implementation of the rule, shared by browsing a Subcategory and by drawing a Study
 * Session, so the two can never disagree about what a filter means (ADR-0038). It takes the pool
 * rather than fetching one: browsing already holds it, and a Session's own use case has just loaded
 * it across several Subcategories.
 *
 * Resolving is immediate today, but that is this implementation's business, not the caller's — a
 * server-side or AI-driven filter would keep the same signature.
 */
class FilterFlashcardsUseCase @Inject constructor() : UseCase<FilterFlashcardsUseCase.Params, FilteredFlashcards> {

    /**
     * @param tagIds OR-**within**: a card matches if it carries any of them. Empty means no tag
     * filter at all, not "match nothing".
     * @param difficultyRange AND-combined with [tagIds], bounds inclusive.
     */
    data class Params(
        val tagIds: Set<String>,
        val difficultyRange: IntRange,
        val pool: List<Flashcard>,
    )

    override suspend operator fun invoke(params: Params): FilteredFlashcards = with(params) {
        FilteredFlashcards(
            cards = pool.filter { card ->
                card.difficulty in difficultyRange && (tagIds.isEmpty() || card.tags.any(tagIds::contains))
            },
            poolTags = pool.flatMap { it.tags }.distinct().sorted(),
            totalCount = pool.size,
        )
    }
}
