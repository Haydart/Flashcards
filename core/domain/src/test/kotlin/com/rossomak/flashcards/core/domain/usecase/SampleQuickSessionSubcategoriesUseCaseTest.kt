package com.rossomak.flashcards.core.domain.usecase

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SampleQuickSessionSubcategoriesUseCaseTest {

    private fun createUseCase(random: Random = Random(FIXED_SEED)): SampleQuickSessionSubcategoriesUseCase =
        SampleQuickSessionSubcategoriesUseCase(random = random)

    private fun params(
        candidateSubcategoryIds: List<String> = CANDIDATE_IDS,
        countRange: IntRange = COUNT_RANGE,
    ): SampleQuickSessionSubcategoriesUseCase.Params = SampleQuickSessionSubcategoriesUseCase.Params(
        candidateSubcategoryIds = candidateSubcategoryIds,
        countRange = countRange,
    )

    @Test
    fun `a fixed random always samples the same subset`() = runTest {
        val firstSample = createUseCase(random = Random(FIXED_SEED))(params())
        val secondSample = createUseCase(random = Random(FIXED_SEED))(params())

        firstSample shouldBe secondSample
    }

    @Test
    fun `the default random produces varying samples across repetitions`() = runTest {
        val samples = (1..10).map { createUseCase(random = Random.Default)(params()) }

        samples.distinct().size shouldNotBe 1
    }

    @Test
    fun `a candidate pool smaller than the range's minimum returns the whole pool`() = runTest {
        val smallPool = listOf("android-compose", "android-coroutines")

        val sample = createUseCase()(params(candidateSubcategoryIds = smallPool, countRange = 3..5))

        sample.toSet() shouldBe smallPool.toSet()
        sample shouldHaveSize smallPool.size
    }

    @Test
    fun `the sampled count always falls within the range once the pool is large enough`() = runTest {
        val useCase = createUseCase(random = Random.Default)
        repeat(50) {
            val sample = useCase(params())

            (sample.size in COUNT_RANGE) shouldBe true
        }
    }

    @Test
    fun `every sampled id comes from the candidate pool`() = runTest {
        val sample = createUseCase()(params())

        sample.forEach { id -> (id in CANDIDATE_IDS) shouldBe true }
    }

    private companion object {
        val CANDIDATE_IDS = (1..31).map { index -> "android-subcategory-$index" }
        val COUNT_RANGE = 3..5
        const val FIXED_SEED = 42L
    }
}
