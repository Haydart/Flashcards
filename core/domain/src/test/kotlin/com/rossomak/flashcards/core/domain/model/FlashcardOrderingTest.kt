package com.rossomak.flashcards.core.domain.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class FlashcardOrderingTest {

    private fun flashcard(id: String, difficulty: Int): Flashcard = Flashcard(
        id = id,
        subcategoryId = "sub-1",
        tags = emptyList(),
        question = "q-$id",
        answer = "a-$id",
        difficulty = difficulty,
        questionCode = null,
        answerCode = null,
        questionSpoken = null,
        answerSpoken = null,
        extendedContext = null,
    )

    private val cards = listOf(
        flashcard(id = "1", difficulty = 5),
        flashcard(id = "2", difficulty = 2),
        flashcard(id = "3", difficulty = 9),
    )

    @Test
    fun `Default leaves the given order untouched`() {
        cards.orderedBy(FlashcardSortOrder.Default).map { it.id } shouldBe listOf("1", "2", "3")
    }

    @Test
    fun `EasiestFirst orders by ascending difficulty`() {
        cards.orderedBy(FlashcardSortOrder.EasiestFirst).map { it.id } shouldBe listOf("2", "1", "3")
    }

    @Test
    fun `HardestFirst orders by descending difficulty`() {
        cards.orderedBy(FlashcardSortOrder.HardestFirst).map { it.id } shouldBe listOf("3", "1", "2")
    }

    @Test
    fun `ordering an empty list yields an empty list`() {
        emptyList<Flashcard>().orderedBy(FlashcardSortOrder.EasiestFirst) shouldBe emptyList()
    }
}
