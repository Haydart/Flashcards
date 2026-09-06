package com.rossomak.flashcards.core.domain.model

import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.Test

class SessionResultTest {

    private fun ledgerEntry(
        cardId: String,
        state: FlashcardProgressState,
        attemptsUsed: Int = 0,
        wasPreviouslyMastered: Boolean = false,
    ): SessionLedgerEntry = SessionLedgerEntry(
        cardId = cardId,
        subcategoryId = "sub-1",
        state = state,
        attemptsUsed = attemptsUsed,
        wasPreviouslyMastered = wasPreviouslyMastered,
    )

    private fun result(ledger: List<SessionLedgerEntry>): SessionResult = SessionResult(
        id = "session-1",
        mode = StudyMode.Rated,
        startedAt = Instant.parse("2026-09-06T10:00:00Z"),
        durationSeconds = 60,
        abandoned = false,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1"),
        subcategoryNames = listOf("Subcategory"),
        ledger = ledger,
    )

    @Test
    fun `a Fast entry carries Seen with zero Attempts`() {
        val entry = ledgerEntry(cardId = "card-1", state = FlashcardProgressState.Seen)

        entry.state shouldBe FlashcardProgressState.Seen
        entry.attemptsUsed shouldBe 0
    }

    @Test
    fun `Mastered, Partial and Failed counts derive from the ledger and cannot disagree with it`() {
        val session = result(
            ledger = listOf(
                ledgerEntry(cardId = "card-1", state = FlashcardProgressState.Mastered, attemptsUsed = 1),
                ledgerEntry(cardId = "card-2", state = FlashcardProgressState.Mastered, attemptsUsed = 2),
                ledgerEntry(cardId = "card-3", state = FlashcardProgressState.Partial, attemptsUsed = 3),
                ledgerEntry(cardId = "card-4", state = FlashcardProgressState.Failed, attemptsUsed = 3),
            ),
        )

        session.masteredCount shouldBe 2
        session.partialCount shouldBe 1
        session.failedCount shouldBe 1
        session.studiedCount shouldBe 4
    }

    @Test
    fun `an empty ledger reports every count as zero`() {
        val session = result(ledger = emptyList())

        session.studiedCount shouldBe 0
        session.masteredCount shouldBe 0
        session.partialCount shouldBe 0
        session.failedCount shouldBe 0
    }
}
