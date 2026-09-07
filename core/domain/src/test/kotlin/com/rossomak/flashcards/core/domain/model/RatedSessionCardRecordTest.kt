package com.rossomak.flashcards.core.domain.model

import com.rossomak.flashcards.core.domain.model.FlashcardRating.Correct
import com.rossomak.flashcards.core.domain.model.FlashcardRating.Failed
import com.rossomak.flashcards.core.domain.model.FlashcardRating.PartiallyCorrect
import io.kotest.matchers.shouldBe
import org.junit.Test

class RatedSessionCardRecordTest {

    private fun flashcard(id: String): Flashcard = Flashcard(
        id = id,
        subcategoryId = "sub-1",
        tags = emptyList(),
        question = "q-$id",
        answer = "a-$id",
        difficulty = 5,
        questionCode = null,
        answerCode = null,
        questionSpoken = null,
        answerSpoken = null,
        extendedContext = null,
    )

    private val card = flashcard("card-1")

    @Test
    fun `a fresh record has no attempts and no best rating`() {
        val record = RatedSessionCardRecord(card = card)

        record.attemptsUsed shouldBe 0
        record.bestRating shouldBe null
    }

    @Test
    fun `ratings are retained in order, including ones that never become the best-so-far`() {
        val ratingSequence = listOf(Failed, PartiallyCorrect, Failed)

        val record = RatedSessionCardRecord(card = card, ratings = ratingSequence)

        record.ratings shouldBe ratingSequence
    }

    @Test
    fun `attempts used equals the rating list size`() {
        val record = RatedSessionCardRecord(card = card, ratings = listOf(Failed, PartiallyCorrect, Failed))

        record.attemptsUsed shouldBe 3
    }

    @Test
    fun `Failed then Partial then Failed resolves best rating to Partial, not the latest rating`() {
        val record = RatedSessionCardRecord(card = card, ratings = listOf(Failed, PartiallyCorrect, Failed))

        record.bestRating shouldBe PartiallyCorrect
    }

    @Test
    fun `a single Correct rating is the best rating regardless of declaration order`() {
        val record = RatedSessionCardRecord(card = card, ratings = listOf(Failed, PartiallyCorrect, Correct))

        record.bestRating shouldBe Correct
    }

    @Test
    fun `an all-Failed history's best rating is Failed`() {
        val record = RatedSessionCardRecord(card = card, ratings = listOf(Failed, Failed))

        record.bestRating shouldBe Failed
    }
}
