package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.model.StudySessionPlan
import com.rossomak.flashcards.core.domain.repository.FlashcardRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Resolves a [StudySessionConfig] into the concrete list of cards a session will run.
 *
 * **The use case owns the fetch.** A caller hands over `{subcategoryIds, filters, length, sort,
 * seed}` — small, and exactly the shape a server-side or AI-driven selector would be POSTed. A
 * variant taking a pre-fetched pool would be easier to test in isolation but would mean uploading
 * the user's whole card pool on every re-selection once that selector exists.
 *
 * Holds an in-memory pool cache keyed on subcategory id, following [SearchCategoriesUseCase]:
 * unscoped, so it lives exactly as long as the ViewModel that injected it. Re-selecting after a
 * dialog confirm therefore costs no read, which is what makes "re-run selection on every confirm"
 * affordable.
 *
 * `suspend` + [Result] from day one even though the local path only fails on the fetch —
 * retrofitting [Result] later would touch every call site.
 */
class SelectSessionFlashcardsUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
) : UseCase<StudySessionConfig, Result<StudySessionPlan>> {

    private val cacheMutex = Mutex()
    private val cachedCardsBySubcategoryId = mutableMapOf<String, List<Flashcard>>()

    override suspend fun invoke(params: StudySessionConfig): Result<StudySessionPlan> =
        loadPool(params.subcategoryIds).map { pool -> buildPlan(select(pool, params), pool) }

    /**
     * [poolTags] comes from the whole pool rather than from [cards]: it is what a filter UI offers
     * as options, and deriving it from the drawn cards would make tags disappear from the picker
     * precisely because the user filtered them out.
     */
    private fun buildPlan(cards: List<Flashcard>, pool: List<Flashcard>): StudySessionPlan = StudySessionPlan(
        cards = cards,
        estimatedMinutes = estimateMinutes(cards.size),
        poolTags = pool.flatMap { it.tags }.distinct().sorted(),
    )

    /** Rounds up so a plan with any cards at all never reads as "~0 min". */
    private fun estimateMinutes(cardCount: Int): Int =
        ((cardCount * SECONDS_PER_CARD) + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE

    private suspend fun loadPool(subcategoryIds: List<String>): Result<List<Flashcard>> = coroutineScope {
        val results = subcategoryIds
            .map { subcategoryId -> async { subcategoryId to fetchCards(subcategoryId) } }
            .awaitAll()
        val failure = results.firstNotNullOfOrNull { (_, result) -> result.exceptionOrNull() }
        if (failure != null) {
            Result.failure(failure)
        } else {
            Result.success(results.flatMap { (_, result) -> result.getOrDefault(emptyList()) })
        }
    }

    private suspend fun fetchCards(subcategoryId: String): Result<List<Flashcard>> {
        val cached = cacheMutex.withLock { cachedCardsBySubcategoryId[subcategoryId] }
        if (cached != null) return Result.success(cached)
        return flashcardRepository.fetchFlashcards(subcategoryId)
            .onSuccess { cards -> cacheMutex.withLock { cachedCardsBySubcategoryId[subcategoryId] = cards } }
    }

    /**
     * Filter, draw, then order. Drawing before ordering is deliberate: sorting the whole eligible
     * pool and taking the head would hand back the same easiest (or hardest) cards every session.
     */
    private fun select(pool: List<Flashcard>, config: StudySessionConfig): List<Flashcard> {
        val drawnCards = pool
            .filter { card -> card.difficulty in config.difficultyRange }
            .filter { card -> config.tagIds.isEmpty() || card.tags.any(config.tagIds::contains) }
            .shuffled(Random(config.seed))
            .take(config.length)
        return when (config.sortOrder) {
            FlashcardSortOrder.Default -> drawnCards
            FlashcardSortOrder.EasiestFirst -> drawnCards.sortedBy { it.difficulty }
            FlashcardSortOrder.HardestFirst -> drawnCards.sortedByDescending { it.difficulty }
        }
    }

    companion object {
        /** Rough pace of a Rated card: read, think, reveal, rate. */
        const val SECONDS_PER_CARD = 40
        private const val SECONDS_PER_MINUTE = 60
    }
}
