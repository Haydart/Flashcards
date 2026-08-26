package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.repository.FakeFlashcardRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SelectSessionFlashcardsUseCaseTest {

    private val flashcardRepository = FakeFlashcardRepository()

    private fun createUseCase(): SelectSessionFlashcardsUseCase =
        SelectSessionFlashcardsUseCase(flashcardRepository)

    private fun config(
        subcategoryIds: List<String> = listOf(SUBCATEGORY_ID),
        length: Int = StudySessionConfig.DEFAULT_LENGTH,
        sortOrder: FlashcardSortOrder = FlashcardSortOrder.Default,
        difficultyRange: IntRange = StudySessionConfig.MIN_DIFFICULTY..StudySessionConfig.MAX_DIFFICULTY,
        tagIds: Set<String> = emptySet(),
        seed: Long = FIXED_SEED,
    ): StudySessionConfig = StudySessionConfig(
        subcategoryIds = subcategoryIds,
        length = length,
        sortOrder = sortOrder,
        difficultyRange = difficultyRange,
        tagIds = tagIds,
        seed = seed,
    )

    private fun flashcard(
        id: String,
        subcategoryId: String = SUBCATEGORY_ID,
        tags: List<String> = listOf("General"),
        difficulty: Int = 5,
    ): Flashcard = Flashcard(
        id = id,
        subcategoryId = subcategoryId,
        tags = tags,
        question = "question-$id",
        answer = "answer-$id",
        difficulty = difficulty,
        questionCode = null,
        answerCode = null,
        questionSpoken = null,
        answerSpoken = null,
        extendedContext = null,
    )

    @Test
    fun `the same seed always draws the same cards in the same order`() = runTest {
        flashcardRepository.flashcardsToReturn =
            Result.success((1..30).map { index -> flashcard(id = "card-$index") })

        val firstPlan = createUseCase().invoke(config()).getOrThrow()
        val secondPlan = createUseCase().invoke(config()).getOrThrow()

        firstPlan.cards shouldBe secondPlan.cards
    }

    @Test
    fun `a different seed draws a different order`() = runTest {
        flashcardRepository.flashcardsToReturn =
            Result.success((1..30).map { index -> flashcard(id = "card-$index") })
        val useCase = createUseCase()

        val firstPlan = useCase(config(seed = FIXED_SEED)).getOrThrow()
        val secondPlan = useCase(config(seed = FIXED_SEED + 1)).getOrThrow()

        firstPlan.cards shouldNotBe secondPlan.cards
    }

    @Test
    fun `length caps the draw`() = runTest {
        flashcardRepository.flashcardsToReturn =
            Result.success((1..30).map { index -> flashcard(id = "card-$index") })

        val plan = createUseCase().invoke(config(length = 10)).getOrThrow()

        plan.cards.size shouldBe 10
    }

    @Test
    fun `a pool smaller than the length is used whole`() = runTest {
        flashcardRepository.flashcardsToReturn =
            Result.success((1..5).map { index -> flashcard(id = "card-$index") })

        val plan = createUseCase().invoke(config()).getOrThrow()

        plan.cards.size shouldBe 5
    }

    @Test
    fun `difficulty range excludes cards outside the band`() = runTest {
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "easy", difficulty = 2),
                flashcard(id = "medium", difficulty = 5),
                flashcard(id = "hard", difficulty = 9),
            )
        )

        val plan = createUseCase().invoke(config(difficultyRange = 4..6)).getOrThrow()

        plan.cards.map { it.id } shouldBe listOf("medium")
    }

    @Test
    fun `tags are OR-within and AND-combined with difficulty`() = runTest {
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "state-easy", tags = listOf("state"), difficulty = 2),
                flashcard(id = "state-medium", tags = listOf("state"), difficulty = 5),
                flashcard(id = "modifiers-medium", tags = listOf("modifiers"), difficulty = 5),
                flashcard(id = "both-medium", tags = listOf("modifiers", "layout"), difficulty = 5),
            )
        )

        val plan = createUseCase().invoke(
            config(tagIds = setOf("state", "layout"), difficultyRange = 4..6)
        ).getOrThrow()

        plan.cards.map { it.id }.sorted() shouldBe listOf("both-medium", "state-medium")
    }

    @Test
    fun `an empty tag set applies no tag filter`() = runTest {
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "card-1", tags = listOf("state")),
                flashcard(id = "card-2", tags = emptyList()),
            )
        )

        val plan = createUseCase().invoke(config()).getOrThrow()

        plan.cards.size shouldBe 2
    }

    @Test
    fun `easiest first sorts the drawn cards ascending by difficulty`() = runTest {
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "hard", difficulty = 8),
                flashcard(id = "easy", difficulty = 2),
                flashcard(id = "medium", difficulty = 5),
            )
        )

        val plan = createUseCase().invoke(config(sortOrder = FlashcardSortOrder.EasiestFirst)).getOrThrow()

        plan.cards.map { it.id } shouldBe listOf("easy", "medium", "hard")
    }

    @Test
    fun `hardest first sorts the drawn cards descending by difficulty`() = runTest {
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "hard", difficulty = 8),
                flashcard(id = "easy", difficulty = 2),
                flashcard(id = "medium", difficulty = 5),
            )
        )

        val plan = createUseCase().invoke(config(sortOrder = FlashcardSortOrder.HardestFirst)).getOrThrow()

        plan.cards.map { it.id } shouldBe listOf("hard", "medium", "easy")
    }

    @Test
    fun `sorting applies to the drawn cards, not to the whole pool`() = runTest {
        // With the pool sorted first, taking the head would always return the three easiest cards.
        flashcardRepository.flashcardsToReturn =
            Result.success((1..30).map { index -> flashcard(id = "card-$index", difficulty = index % 10 + 1) })

        val plan = createUseCase().invoke(
            config(length = 3, sortOrder = FlashcardSortOrder.EasiestFirst)
        ).getOrThrow()

        plan.cards.size shouldBe 3
        plan.cards.map { it.difficulty } shouldBe plan.cards.map { it.difficulty }.sorted()
        plan.cards.map { it.difficulty } shouldNotBe listOf(1, 1, 1)
    }

    @Test
    fun `cards are pooled across every routed subcategory`() = runTest {
        flashcardRepository.flashcardsBySubcategory[SUBCATEGORY_ID] =
            Result.success(listOf(flashcard(id = "compose-1")))
        flashcardRepository.flashcardsBySubcategory[OTHER_SUBCATEGORY_ID] =
            Result.success(listOf(flashcard(id = "coroutines-1", subcategoryId = OTHER_SUBCATEGORY_ID)))

        val plan = createUseCase().invoke(
            config(subcategoryIds = listOf(SUBCATEGORY_ID, OTHER_SUBCATEGORY_ID))
        ).getOrThrow()

        plan.cards.map { it.id }.sorted() shouldBe listOf("compose-1", "coroutines-1")
    }

    @Test
    fun `a failing fetch fails the whole selection`() = runTest {
        val error = IllegalStateException("boom")
        flashcardRepository.flashcardsBySubcategory[SUBCATEGORY_ID] =
            Result.success(listOf(flashcard(id = "compose-1")))
        flashcardRepository.flashcardsBySubcategory[OTHER_SUBCATEGORY_ID] = Result.failure(error)

        val result = createUseCase().invoke(config(subcategoryIds = listOf(SUBCATEGORY_ID, OTHER_SUBCATEGORY_ID)))

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
    }

    @Test
    fun `pool tags are the whole pool's vocabulary, not the drawn cards'`() = runTest {
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "card-1", tags = listOf("state")),
                flashcard(id = "card-2", tags = listOf("modifiers", "state")),
            )
        )

        val plan = createUseCase().invoke(config(tagIds = setOf("state"), length = 1)).getOrThrow()

        plan.cards.size shouldBe 1
        plan.poolTags shouldBe listOf("modifiers", "state")
    }

    @Test
    fun `estimated minutes round up`() = runTest {
        flashcardRepository.flashcardsToReturn =
            Result.success((1..5).map { index -> flashcard(id = "card-$index") })

        val plan = createUseCase().invoke(config()).getOrThrow()

        // 5 cards * 40s = 200s -> ceil(200 / 60) = 4
        SelectSessionFlashcardsUseCase.SECONDS_PER_CARD shouldBe 40
        plan.estimatedMinutes shouldBe 4
    }

    @Test
    fun `a repeated selection is served from the pool cache`() = runTest {
        flashcardRepository.flashcardsToReturn =
            Result.success((1..30).map { index -> flashcard(id = "card-$index") })
        val useCase = createUseCase()
        useCase(config()).getOrThrow()

        flashcardRepository.flashcardsToReturn = Result.failure(IllegalStateException("must not re-fetch"))
        val plan = useCase(config(length = 5)).getOrThrow()

        plan.cards.size shouldBe 5
    }

    private companion object {
        const val SUBCATEGORY_ID = "android-compose"
        const val OTHER_SUBCATEGORY_ID = "android-coroutines"
        const val FIXED_SEED = 42L
    }
}
