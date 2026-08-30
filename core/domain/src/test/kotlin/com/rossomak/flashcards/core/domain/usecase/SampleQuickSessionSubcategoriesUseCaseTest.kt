package com.rossomak.flashcards.core.domain.usecase

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SampleQuickSessionSubcategoriesUseCaseTest {

    private val useCase = SampleQuickSessionSubcategoriesUseCase()

    private fun params(
        candidateSubcategoryIds: List<String> = CANDIDATE_IDS,
        countRange: IntRange = COUNT_RANGE,
        seed: Long = FIXED_SEED,
    ): SampleQuickSessionSubcategoriesUseCase.Params = SampleQuickSessionSubcategoriesUseCase.Params(
        candidateSubcategoryIds = candidateSubcategoryIds,
        countRange = countRange,
        seed = seed,
    )

    @Test
    fun `the same seed always samples the same subset`() = runTest {
        val firstSample = useCase(params())
        val secondSample = useCase(params())

        firstSample shouldBe secondSample
    }

    @Test
    fun `a different seed samples a different subset`() = runTest {
        val firstSample = useCase(params(seed = FIXED_SEED))
        val secondSample = useCase(params(seed = FIXED_SEED + 1))

        firstSample shouldNotBe secondSample
    }

    @Test
    fun `a candidate pool smaller than the range's minimum returns the whole pool`() = runTest {
        val smallPool = listOf("android-compose", "android-coroutines")

        val sample = useCase(params(candidateSubcategoryIds = smallPool, countRange = 3..5))

        sample.toSet() shouldBe smallPool.toSet()
        sample shouldHaveSize smallPool.size
    }

    @Test
    fun `the sampled count always falls within the range once the pool is large enough`() = runTest {
        (0 until 50).forEach { seed ->
            val sample = useCase(params(seed = seed.toLong()))

            (sample.size in COUNT_RANGE) shouldBe true
        }
    }

    @Test
    fun `every sampled id comes from the candidate pool`() = runTest {
        val sample = useCase(params())

        sample.forEach { id -> (id in CANDIDATE_IDS) shouldBe true }
    }

    private companion object {
        val CANDIDATE_IDS = (1..31).map { index -> "android-subcategory-$index" }
        val COUNT_RANGE = 3..5
        const val FIXED_SEED = 42L
    }
}
