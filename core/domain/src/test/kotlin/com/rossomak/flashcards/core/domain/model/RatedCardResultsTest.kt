package com.rossomak.flashcards.core.domain.model

import com.rossomak.flashcards.core.domain.model.FlashcardAttemptRating.Correct
import com.rossomak.flashcards.core.domain.model.FlashcardAttemptRating.Failed
import com.rossomak.flashcards.core.domain.model.FlashcardAttemptRating.PartiallyCorrect
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import org.junit.Test

class RatedCardResultsTest {

    private fun flashcard(id: String, subcategoryId: String = "sub-1"): Flashcard = Flashcard(
        id = id,
        subcategoryId = subcategoryId,
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

    private fun state(cardCount: Int, attemptsLimit: Int = 3): RatedSessionState = RatedSessionState.seed(
        cards = (1..cardCount).map { flashcard("card-$it") },
        attemptsLimit = attemptsLimit,
        random = Random(42),
    )

    @Test
    fun `FlashcardTerminalRating Correct-only maps to Mastered via toAbandonedFlashcardTerminalRating`() {
        Correct.toAbandonedFlashcardTerminalRating() shouldBe FlashcardTerminalRating.Mastered
    }

    @Test
    fun `FlashcardTerminalRating PartiallyCorrect maps to Partial via toAbandonedFlashcardTerminalRating`() {
        PartiallyCorrect.toAbandonedFlashcardTerminalRating() shouldBe FlashcardTerminalRating.Partial
    }

    @Test
    fun `FlashcardTerminalRating Failed maps to Failed via toAbandonedFlashcardTerminalRating`() {
        Failed.toAbandonedFlashcardTerminalRating() shouldBe FlashcardTerminalRating.Failed
    }

    @Test
    fun `a naturally completed session seals one card result per distinct card with its Terminal State`() {
        val afterCard1 = rate(state(cardCount = 2, attemptsLimit = 1), Correct)
        val afterCard2 = rate(afterCard1.state, Failed)

        val cardResults = sealRatedCardResults(afterCard2.state, abandoned = false)

        cardResults shouldContainExactlyInAnyOrder listOf(
            FlashcardResult.Rated(
                cardId = "card-1",
                subcategoryId = "sub-1",
                state = FlashcardStudyProgressState.Mastered,
                attemptsUsed = 1,
                wasPreviouslyMastered = false,
            ),
            FlashcardResult.Rated(
                cardId = "card-2",
                subcategoryId = "sub-1",
                state = FlashcardStudyProgressState.Failed,
                attemptsUsed = 1,
                wasPreviouslyMastered = false,
            ),
        )
    }

    @Test
    fun `a card drawn but never reached is absent from cardResults even when abandoned`() {
        val session = state(cardCount = 2)

        val cardResults = sealRatedCardResults(session, abandoned = true)

        cardResults.shouldBeEmpty()
    }

    @Test
    fun `a card that received only a silence timeout is absent from cardResults`() {
        val session = requeueAfterSilence(state(cardCount = 1))

        val cardResults = sealRatedCardResults(session, abandoned = true)

        cardResults.shouldBeEmpty()
    }

    @Test
    fun `abandon force-resolves a card mid re-insertion using its best-rating-so-far, not the existing resolveFlashcardTerminalRating path`() {
        // Failed then Partial: still queued (attemptsLimit 3 not exhausted), best rating is Partial.
        val afterFailed = rate(state(cardCount = 1, attemptsLimit = 3), Failed)
        val afterPartial = rate(afterFailed.state, PartiallyCorrect)
        afterPartial.terminal shouldBe null

        val cardResults = sealRatedCardResults(afterPartial.state, abandoned = true)

        cardResults shouldContainExactlyInAnyOrder listOf(
            FlashcardResult.Rated(
                cardId = "card-1",
                subcategoryId = "sub-1",
                state = FlashcardStudyProgressState.Partial,
                attemptsUsed = 2,
                wasPreviouslyMastered = false,
            ),
        )
    }

    @Test
    fun `a force-resolved card is absent when the session is not abandoned, since natural end never leaves an unresolved card queued`() {
        val afterFailed = rate(state(cardCount = 1, attemptsLimit = 3), Failed)

        val cardResults = sealRatedCardResults(afterFailed.state, abandoned = false)

        cardResults.shouldBeEmpty()
    }

    @Test
    fun `wasPreviouslyMastered is threaded through to the card result, both resolved and force-resolved`() {
        val previouslyMasteredCard = flashcard("card-1")
        val seeded = RatedSessionState(
            queue = listOf(RatedSessionCardRecord(card = previouslyMasteredCard, wasPreviouslyMastered = true)),
            attemptsLimit = 3,
        )

        val afterPartial = rate(seeded, PartiallyCorrect)
        val forcedResults = sealRatedCardResults(afterPartial.state, abandoned = true)
        forcedResults.single().wasPreviouslyMastered shouldBe true

        val afterCorrect = rate(afterPartial.state, Correct)
        val resolvedResults = sealRatedCardResults(afterCorrect.state, abandoned = false)
        resolvedResults.single().wasPreviouslyMastered shouldBe true
    }
}
