package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject
import kotlin.random.Random

/**
 * Samples a bounded random subset of a Category's Subcategories for a Quick Session, instead of
 * pulling every one of them into the session's pool (ADR-0040).
 *
 * Pure and synchronous — no repository dependency — since it only shuffles ids the caller already
 * holds. Called only from the Quick Session path: single-Subcategory and Custom sessions hand
 * [SelectSessionFlashcardsUseCase] a fixed Subcategory list and never go through this use case,
 * because Quick is the only scenario where the Subcategory set itself can change between
 * resolutions.
 *
 * The sampled ids are held as screen state, not re-derived per selection: the caller samples once
 * on load and once on Re-randomise, and reuses the result for every other selection (ADR-0040).
 *
 * [random] drives the sample — `Random.Default` in production, bound in `RandomModule`, and a
 * fixed `Random` in tests, injected directly. There is no session seed.
 */
class SampleQuickSessionSubcategoriesUseCase @Inject constructor(
    private val random: Random,
) : UseCase<SampleQuickSessionSubcategoriesUseCase.Params, List<String>> {

    /**
     * @param candidateSubcategoryIds every Subcategory the sample may be drawn from.
     * @param countRange bounds on how many Subcategories to sample.
     */
    data class Params(
        val candidateSubcategoryIds: List<String>,
        val countRange: IntRange,
    )

    /**
     * When the candidate pool is smaller than [Params.countRange]'s minimum, the whole pool is
     * used silently — no warning, no error, the same idiom [SelectSessionFlashcardsUseCase] uses
     * when a card pool is smaller than the session length.
     *
     * The range is clamped to the pool size **before** drawing, not after: clamping a drawn value
     * biases the feasible counts toward the pool size (e.g. a `3..5` range over a 4-candidate pool
     * would draw 4 twice as often as 3, since both 4 and 5 collapse to 4). Clamping the bounds
     * first keeps every feasible count equally likely.
     */
    override suspend fun invoke(params: Params): List<String> {
        val poolSize = params.candidateSubcategoryIds.size
        val lowerBound = params.countRange.first.coerceAtMost(poolSize)
        val upperBound = params.countRange.last.coerceAtMost(poolSize)
        val count = random.nextInt(lowerBound, upperBound + 1)
        return params.candidateSubcategoryIds.shuffled(random).take(count)
    }
}
