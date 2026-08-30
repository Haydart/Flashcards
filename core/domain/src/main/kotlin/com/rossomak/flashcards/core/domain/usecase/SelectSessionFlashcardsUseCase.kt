package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.FilteredFlashcards
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.model.StudySessionPlan
import com.rossomak.flashcards.core.domain.model.orderedBy
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Resolves a [StudySessionConfig] into the concrete list of cards a session will run.
 *
 * A composite: it loads each Subcategory's pool through [GetFlashcardsUseCase], narrows it with the
 * shared [FilterFlashcardsUseCase], then draws and orders. It owns none of those three steps — the
 * filter in particular is shared with browsing, so a filter can never mean one thing in a list and
 * another in a session (ADR-0038).
 *
 * **It holds no cache.** Repeat reads of the same Subcategory are the repository's problem, which is
 * what keeps "re-run selection on every dialog confirm" affordable without this use case knowing
 * anything about caching.
 *
 * Session-only. Browsing a Subcategory does not come through here: a browsed list is complete and
 * stably ordered, where a session draw is capped and shuffled.
 */
class SelectSessionFlashcardsUseCase @Inject constructor(
    private val getFlashcards: GetFlashcardsUseCase,
    private val filterFlashcards: FilterFlashcardsUseCase,
) : UseCase<StudySessionConfig, Result<StudySessionPlan>> {

    /**
     * Not `mapCatching`: [buildPlan] suspends, and mapCatching would turn a `CancellationException`
     * into a failed [Result] instead of letting it propagate.
     */
    override suspend fun invoke(params: StudySessionConfig): Result<StudySessionPlan> {
        val pool = loadPool(params.subcategoryIds).getOrElse { failure -> return Result.failure(failure) }
        return Result.success(buildPlan(pool, params))
    }

    private suspend fun buildPlan(pool: List<Flashcard>, config: StudySessionConfig): StudySessionPlan {
        val filtered = filterFlashcards(
            FilterFlashcardsUseCase.Params(
                tagIds = config.tagIds,
                difficultyRange = config.difficultyRange,
                pool = pool,
            )
        )
        val drawn = draw(filtered, config)
        return StudySessionPlan(
            cards = drawn,
            estimatedMinutes = estimateMinutes(drawn.size),
            // From the whole pool, not the draw: these are what a filter picker offers, and deriving
            // them from the drawn cards would hide the very tags the user filtered on.
            poolTags = filtered.poolTags,
        )
    }

    /**
     * Draw, then order. Drawing first is deliberate: ordering the whole eligible pool and taking the
     * head would hand back the same easiest — or hardest — cards every session.
     */
    private fun draw(filtered: FilteredFlashcards, config: StudySessionConfig): List<Flashcard> =
        filtered.cards
            .shuffled(Random(config.seed))
            .take(config.length)
            .orderedBy(config.sortOrder)

    /** Rounds up so a plan with any cards at all never reads as "~0 min". */
    private fun estimateMinutes(cardCount: Int): Int =
        ((cardCount * SECONDS_PER_CARD) + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE

    /**
     * Loads every Subcategory in parallel and flattens them into one pool. The first failure wins:
     * a partial pool would silently narrow the session rather than reporting that something is
     * wrong.
     */
    private suspend fun loadPool(subcategoryIds: List<String>): Result<List<Flashcard>> = coroutineScope {
        val results = subcategoryIds
            .map { subcategoryId -> async { getFlashcards(subcategoryId) } }
            .awaitAll()
        val failure = results.firstNotNullOfOrNull { result -> result.exceptionOrNull() }
        if (failure != null) {
            Result.failure(failure)
        } else {
            Result.success(results.flatMap { result -> result.getOrDefault(emptyList()) })
        }
    }

    companion object {
        /** Rough pace of a Rated card: read, think, reveal, rate. */
        const val SECONDS_PER_CARD = 40
        private const val SECONDS_PER_MINUTE = 60
    }
}
