package com.rossomak.flashcards.core.domain.model

import com.rossomak.flashcards.core.domain.model.FlashcardRating.Correct
import com.rossomak.flashcards.core.domain.model.FlashcardRating.Failed
import com.rossomak.flashcards.core.domain.model.FlashcardRating.PartiallyCorrect
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.random.Random
import org.junit.Test

class RatedSessionStateTest {

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

    private fun cards(count: Int): List<Flashcard> = (1..count).map { flashcard("card-$it") }

    private fun state(
        cardCount: Int,
        attemptsLimit: Int = DEFAULT_ATTEMPTS_LIMIT,
        partialRatingCardRequeueingEnabled: Boolean = true,
        random: Random = Random(FIXED_SEED),
    ): RatedSessionState = RatedSessionState(
        cards = cards(cardCount),
        attemptsLimit = attemptsLimit,
        partialRatingCardRequeueingEnabled = partialRatingCardRequeueingEnabled,
        random = random,
    )

    @Test
    fun `Correct on the first Attempt finishes the card as Mastered`() {
        val session = state(cardCount = 1)

        val terminal = session.rate(Correct)

        terminal shouldBe TerminalState.Mastered
        session.isComplete shouldBe true
    }

    @Test
    fun `Correct on a later Attempt still finishes the card as Mastered`() {
        val session = state(cardCount = 1, attemptsLimit = 3)

        session.rate(Failed) shouldBe null
        val terminal = session.rate(Correct)

        terminal shouldBe TerminalState.Mastered
    }

    @Test
    fun `Failed then Partial then Failed resolves to Terminal Partial, not Terminal Failed`() {
        val session = state(cardCount = 1, attemptsLimit = 3)

        session.rate(Failed) shouldBe null
        session.rate(PartiallyCorrect) shouldBe null
        val terminal = session.rate(Failed)

        terminal shouldBe TerminalState.Partial
    }

    @Test
    fun `exhausting Attempts having only ever rated Failed resolves to Terminal Failed`() {
        val session = state(cardCount = 1, attemptsLimit = 2)

        session.rate(Failed) shouldBe null
        val terminal = session.rate(Failed)

        terminal shouldBe TerminalState.Failed
    }

    @Test
    fun `exhausting Attempts having been Partial at least once resolves to Terminal Partial`() {
        val session = state(cardCount = 1, attemptsLimit = 2)

        session.rate(PartiallyCorrect) shouldBe null
        val terminal = session.rate(Failed)

        terminal shouldBe TerminalState.Partial
    }

    @Test
    fun `a Partial rating is immediately Terminal Partial when partial requeueing is disabled`() {
        val session = state(cardCount = 1, attemptsLimit = 5, partialRatingCardRequeueingEnabled = false)

        val terminal = session.rate(PartiallyCorrect)

        terminal shouldBe TerminalState.Partial
        session.isComplete shouldBe true
    }

    @Test
    fun `a Partial rating re-inserts as normal when partial requeueing is enabled`() {
        val session = state(cardCount = 10, attemptsLimit = 5)

        val terminal = session.rate(PartiallyCorrect)

        terminal shouldBe null
        session.isComplete shouldBe false
        session.remainingCards.map { it.id } shouldContain FIRST_CARD_ID
    }

    @Test
    fun `a card at its Attempts limit is never re-inserted, even with other cards still queued`() {
        val session = state(cardCount = 3, attemptsLimit = 1)

        session.rate(Failed)

        session.remainingCards.map { it.id } shouldNotContain FIRST_CARD_ID
    }

    @Test
    fun `an Attempts limit of 1 makes every rating immediately terminal`() {
        listOf(Failed, PartiallyCorrect, Correct).forEach { rating ->
            val session = state(cardCount = 1, attemptsLimit = 1)

            val terminal = session.rate(rating)

            terminal shouldNotBe null
            session.isComplete shouldBe true
        }
    }

    @Test
    fun `a Failed re-insertion always lands at index 2 to 4, never 0 or 1`() {
        repeat(REPETITIONS) {
            val session = state(cardCount = LARGE_POOL_SIZE, random = Random.Default)

            session.rate(Failed)

            val index = session.remainingCards.indexOfFirst { it.id == FIRST_CARD_ID }
            (index in StudySessionConfig.FAILED_REQUEUE_MIN_GAP..StudySessionConfig.FAILED_REQUEUE_MAX_GAP) shouldBe true
        }
    }

    @Test
    fun `a Partial re-insertion always lands at index 5 to 9, never 0 or 1`() {
        repeat(REPETITIONS) {
            val session = state(cardCount = LARGE_POOL_SIZE, random = Random.Default)

            session.rate(PartiallyCorrect)

            val index = session.remainingCards.indexOfFirst { it.id == FIRST_CARD_ID }
            (index in StudySessionConfig.PARTIAL_REQUEUE_MIN_GAP..StudySessionConfig.PARTIAL_REQUEUE_MAX_GAP) shouldBe true
        }
    }

    @Test
    fun `Failed re-insertion gap varies across repetitions rather than being fixed`() {
        val indices = (1..REPETITIONS).map {
            val session = state(cardCount = LARGE_POOL_SIZE, random = Random.Default)
            session.rate(Failed)
            session.remainingCards.indexOfFirst { it.id == FIRST_CARD_ID }
        }

        indices.distinct().size shouldNotBe 1
    }

    @Test
    fun `a re-insertion into a queue shorter than the drawn gap appends at the end`() {
        // Partial's minimum gap (5) exceeds the 2 cards left once the head is removed.
        val session = state(cardCount = 3, attemptsLimit = 5, random = Random.Default)

        session.rate(PartiallyCorrect)

        session.remainingCards.last().id shouldBe FIRST_CARD_ID
    }

    @Test
    fun `a fixed Random produces an identical queue sequence for an identical rating sequence`() {
        val ratingSequence = listOf(Failed, PartiallyCorrect, Failed, Correct, Failed)

        val firstSession = state(cardCount = LARGE_POOL_SIZE, attemptsLimit = 4, random = Random(FIXED_SEED))
        val secondSession = state(cardCount = LARGE_POOL_SIZE, attemptsLimit = 4, random = Random(FIXED_SEED))

        val firstOrder = ratingSequence.map { rating -> firstSession.currentCard?.id.also { firstSession.rate(rating) } }
        val secondOrder = ratingSequence.map { rating -> secondSession.currentCard?.id.also { secondSession.rate(rating) } }

        firstOrder shouldBe secondOrder
    }

    @Test
    fun `distinct card count and mastered count do not move on a re-insertion`() {
        val session = state(cardCount = 5, attemptsLimit = 3)
        val distinctBefore = session.distinctCardCount
        val masteredBefore = session.masteredCount

        session.rate(Failed)

        session.distinctCardCount shouldBe distinctBefore
        session.masteredCount shouldBe masteredBefore
    }

    @Test
    fun `mastered count increases only on a Terminal Mastered`() {
        val session = state(cardCount = 2, attemptsLimit = 1)

        session.rate(Failed)
        session.masteredCount shouldBe 0

        session.rate(Correct)
        session.masteredCount shouldBe 1
    }

    @Test
    fun `the session reports complete exactly when every distinct card is terminal`() {
        val session = state(cardCount = 2, attemptsLimit = 1)

        session.isComplete shouldBe false
        session.rate(Correct)
        session.isComplete shouldBe false
        session.rate(Correct)
        session.isComplete shouldBe true
    }

    private companion object {
        const val DEFAULT_ATTEMPTS_LIMIT = 3
        const val FIXED_SEED = 42L
        const val REPETITIONS = 200
        const val LARGE_POOL_SIZE = 20
        const val FIRST_CARD_ID = "card-1"
    }
}
