package com.rossomak.flashcards.core.domain.model

import com.rossomak.flashcards.core.domain.model.FlashcardRating.Correct
import com.rossomak.flashcards.core.domain.model.FlashcardRating.Failed
import com.rossomak.flashcards.core.domain.model.FlashcardRating.PartiallyCorrect
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import org.junit.Test

class RatedLedgerTest {

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
    fun `TerminalState Correct-only maps to Mastered via toAbandonedTerminalState`() {
        Correct.toAbandonedTerminalState() shouldBe TerminalState.Mastered
    }

    @Test
    fun `TerminalState PartiallyCorrect maps to Partial via toAbandonedTerminalState`() {
        PartiallyCorrect.toAbandonedTerminalState() shouldBe TerminalState.Partial
    }

    @Test
    fun `TerminalState Failed maps to Failed via toAbandonedTerminalState`() {
        Failed.toAbandonedTerminalState() shouldBe TerminalState.Failed
    }

    @Test
    fun `a naturally completed session seals one ledger entry per distinct card with its Terminal State`() {
        val afterCard1 = rate(state(cardCount = 2, attemptsLimit = 1), Correct)
        val afterCard2 = rate(afterCard1.state, Failed)

        val ledger = sealRatedLedger(afterCard2.state, abandoned = false)

        ledger shouldContainExactlyInAnyOrder listOf(
            SessionLedgerEntry(
                cardId = "card-1",
                subcategoryId = "sub-1",
                state = FlashcardProgressState.Mastered,
                attemptsUsed = 1,
                wasPreviouslyMastered = false,
            ),
            SessionLedgerEntry(
                cardId = "card-2",
                subcategoryId = "sub-1",
                state = FlashcardProgressState.Failed,
                attemptsUsed = 1,
                wasPreviouslyMastered = false,
            ),
        )
    }

    @Test
    fun `a card drawn but never reached is absent from the ledger even when abandoned`() {
        val session = state(cardCount = 2)

        val ledger = sealRatedLedger(session, abandoned = true)

        ledger.shouldBeEmpty()
    }

    @Test
    fun `a card that received only a silence timeout is absent from the ledger`() {
        val session = requeueAfterSilence(state(cardCount = 1))

        val ledger = sealRatedLedger(session, abandoned = true)

        ledger.shouldBeEmpty()
    }

    @Test
    fun `abandon force-resolves a card mid re-insertion using its best-rating-so-far, not the existing resolveTerminalState path`() {
        // Failed then Partial: still queued (attemptsLimit 3 not exhausted), best rating is Partial.
        val afterFailed = rate(state(cardCount = 1, attemptsLimit = 3), Failed)
        val afterPartial = rate(afterFailed.state, PartiallyCorrect)
        afterPartial.terminal shouldBe null

        val ledger = sealRatedLedger(afterPartial.state, abandoned = true)

        ledger shouldContainExactlyInAnyOrder listOf(
            SessionLedgerEntry(
                cardId = "card-1",
                subcategoryId = "sub-1",
                state = FlashcardProgressState.Partial,
                attemptsUsed = 2,
                wasPreviouslyMastered = false,
            ),
        )
    }

    @Test
    fun `a force-resolved card is absent when the session is not abandoned, since natural end never leaves an unresolved card queued`() {
        val afterFailed = rate(state(cardCount = 1, attemptsLimit = 3), Failed)

        val ledger = sealRatedLedger(afterFailed.state, abandoned = false)

        ledger.shouldBeEmpty()
    }

    @Test
    fun `wasPreviouslyMastered is threaded through to the ledger entry, both resolved and force-resolved`() {
        val previouslyMasteredCard = flashcard("card-1")
        val seeded = RatedSessionState(
            queue = listOf(RatedSessionCardRecord(card = previouslyMasteredCard, wasPreviouslyMastered = true)),
            attemptsLimit = 3,
        )

        val afterPartial = rate(seeded, PartiallyCorrect)
        val forcedLedger = sealRatedLedger(afterPartial.state, abandoned = true)
        forcedLedger.single().wasPreviouslyMastered shouldBe true

        val afterCorrect = rate(afterPartial.state, Correct)
        val resolvedLedger = sealRatedLedger(afterCorrect.state, abandoned = false)
        resolvedLedger.single().wasPreviouslyMastered shouldBe true
    }
}
