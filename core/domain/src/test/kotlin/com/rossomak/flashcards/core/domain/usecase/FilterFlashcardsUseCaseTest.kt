package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.Flashcard
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FilterFlashcardsUseCaseTest {

    private val filterFlashcards = FilterFlashcardsUseCase()

    private fun flashcard(
        id: String,
        difficulty: Int,
        tags: List<String> = emptyList(),
    ): Flashcard = Flashcard(
        id = id,
        subcategoryId = "sub-1",
        tags = tags,
        question = "q-$id",
        answer = "a-$id",
        difficulty = difficulty,
        questionCode = null,
        answerCode = null,
        questionSpoken = null,
        answerSpoken = null,
        extendedContext = null,
    )

    private val pool = listOf(
        flashcard(id = "1", difficulty = 2, tags = listOf("State")),
        flashcard(id = "2", difficulty = 5, tags = listOf("Modifiers", "State")),
        flashcard(id = "3", difficulty = 8, tags = listOf("Theming")),
        flashcard(id = "4", difficulty = 10, tags = emptyList()),
    )

    private suspend fun filter(
        tagIds: Set<String> = emptySet(),
        difficultyRange: IntRange = 1..10,
        cards: List<Flashcard> = pool,
    ) = filterFlashcards(
        FilterFlashcardsUseCase.Params(tagIds = tagIds, difficultyRange = difficultyRange, pool = cards)
    )

    @Test
    fun `empty tag set applies no tag filter`() = runTest {
        filter().cards.map { it.id } shouldBe listOf("1", "2", "3", "4")
    }

    @Test
    fun `multiple tags match a card carrying any one of them`() = runTest {
        filter(tagIds = setOf("Modifiers", "Theming")).cards.map { it.id } shouldBe listOf("2", "3")
    }

    @Test
    fun `difficulty range and tags combine with and`() = runTest {
        filter(tagIds = setOf("State"), difficultyRange = 4..10).cards.map { it.id } shouldBe listOf("2")
    }

    @Test
    fun `difficulty range bounds are inclusive`() = runTest {
        filter(difficultyRange = 2..8).cards.map { it.id } shouldBe listOf("1", "2", "3")
    }

    @Test
    fun `filtering to nothing yields no cards but still reports the pool`() = runTest {
        val filtered = filter(tagIds = setOf("Theming"), difficultyRange = 1..3)

        filtered.cards shouldBe emptyList()
        filtered.totalCount shouldBe 4
        filtered.poolTags shouldBe listOf("Modifiers", "State", "Theming")
    }

    @Test
    fun `pool tags come from the whole pool and survive filtering those tags out`() = runTest {
        val filtered = filter(tagIds = setOf("State"))

        filtered.cards.map { it.id } shouldBe listOf("1", "2")
        filtered.poolTags shouldBe listOf("Modifiers", "State", "Theming")
    }

    @Test
    fun `pool tags are distinct and sorted`() = runTest {
        filter().poolTags shouldBe listOf("Modifiers", "State", "Theming")
    }

    @Test
    fun `total count is the unfiltered pool size`() = runTest {
        val filtered = filter(tagIds = setOf("Theming"))

        filtered.cards.size shouldBe 1
        filtered.totalCount shouldBe 4
    }

    @Test
    fun `cards keep the pool order`() = runTest {
        filter(cards = pool.asReversed()).cards.map { it.id } shouldBe listOf("4", "3", "2", "1")
    }

    @Test
    fun `an empty pool yields no cards no tags and a zero total`() = runTest {
        val filtered = filter(cards = emptyList())

        filtered.cards shouldBe emptyList()
        filtered.poolTags shouldBe emptyList()
        filtered.totalCount shouldBe 0
    }
}
