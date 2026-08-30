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
 * Seeded off the same [com.rossomak.flashcards.core.domain.model.StudySessionConfig.seed] the
 * card draw uses, so one seed reproduces the whole plan — the Subcategory sample and the card draw
 * within it alike.
 */
class SampleQuickSessionSubcategoriesUseCase @Inject constructor() :
    UseCase<SampleQuickSessionSubcategoriesUseCase.Params, List<String>> {

    /**
     * @param candidateSubcategoryIds every Subcategory the sample may be drawn from.
     * @param countRange bounds on how many Subcategories to sample.
     * @param seed drives both the sampled count and the subset itself.
     */
    data class Params(
        val candidateSubcategoryIds: List<String>,
        val countRange: IntRange,
        val seed: Long,
    )

    /**
     * When the candidate pool is smaller than [Params.countRange]'s minimum, the whole pool is
     * used silently — no warning, no error, the same idiom [SelectSessionFlashcardsUseCase] uses
     * when a card pool is smaller than the session length.
     */
    override suspend fun invoke(params: Params): List<String> {
        val random = Random(params.seed)
        val count = random.nextInt(params.countRange.first, params.countRange.last + 1)
            .coerceAtMost(params.candidateSubcategoryIds.size)
        return params.candidateSubcategoryIds.shuffled(random).take(count)
    }
}
