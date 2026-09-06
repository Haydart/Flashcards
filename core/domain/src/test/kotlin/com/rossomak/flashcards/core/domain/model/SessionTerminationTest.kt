package com.rossomak.flashcards.core.domain.model

import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.Test

class SessionTerminationTest {

    private val startedAt = Instant.parse("2026-09-06T10:00:00Z")

    private fun ledgerEntry(cardId: String): SessionLedgerEntry = SessionLedgerEntry(
        cardId = cardId,
        subcategoryId = "sub-1",
        state = FlashcardProgressState.Seen,
        attemptsUsed = 0,
        wasPreviouslyMastered = false,
    )

    private fun placeholderResult(ledger: List<SessionLedgerEntry> = listOf(ledgerEntry("card-1"))): SessionResult =
        SessionResult(
            id = "session-1",
            mode = StudyMode.Rated,
            startedAt = startedAt,
            durationSeconds = -1, // deliberately wrong, so a passing test proves it was overwritten
            abandoned = false,
            categoryId = "cat-1",
            categoryName = "Category",
            subcategoryIds = listOf("sub-1"),
            subcategoryNames = listOf("Subcategory"),
            ledger = ledger,
        )

    @Test
    fun `sealSessionResult stops the clock and overwrites durationSeconds with its elapsed seconds`() {
        val runningClock = startClock(SessionClock(), startedAt)

        val result = sealSessionResult(
            result = placeholderResult(),
            clock = runningClock,
            at = startedAt.plusSeconds(90),
        )

        result.durationSeconds shouldBe 90
    }

    @Test
    fun `sealSessionResult carries every other field of result straight through unchanged`() {
        val ledger = listOf(ledgerEntry("card-1"), ledgerEntry("card-2"))
        val placeholder = placeholderResult(ledger).copy(abandoned = true)

        val result = sealSessionResult(result = placeholder, clock = SessionClock(), at = startedAt)

        result.copy(durationSeconds = placeholder.durationSeconds) shouldBe placeholder
    }

    @Test
    fun `sealSessionResult on a clock that never started reports zero duration`() {
        val result = sealSessionResult(
            result = placeholderResult(ledger = emptyList()),
            clock = SessionClock(),
            at = startedAt.plusSeconds(999),
        )

        result.durationSeconds shouldBe 0
    }
}
