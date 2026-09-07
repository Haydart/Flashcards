package com.rossomak.flashcards.core.domain.model

import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.Test

class SessionResultTest {

    private fun cardResult(
        cardId: String,
        state: FlashcardStudyProgressState,
        attemptsUsed: Int = 0,
        wasPreviouslyMastered: Boolean = false,
    ): FlashcardResult = FlashcardResult(
        cardId = cardId,
        subcategoryId = "sub-1",
        state = state,
        attemptsUsed = attemptsUsed,
        wasPreviouslyMastered = wasPreviouslyMastered,
    )

    private fun result(cardResults: List<FlashcardResult>): SessionResult = SessionResult(
        id = "session-1",
        mode = StudyMode.Rated,
        startedAt = Instant.parse("2026-09-06T10:00:00Z"),
        durationSeconds = 60,
        abandoned = false,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1"),
        subcategoryNames = listOf("Subcategory"),
        cardResults = cardResults,
    )

    @Test
    fun `a Fast entry carries Seen with zero Attempts`() {
        val entry = cardResult(cardId = "card-1", state = FlashcardStudyProgressState.Seen)

        entry.state shouldBe FlashcardStudyProgressState.Seen
        entry.attemptsUsed shouldBe 0
    }

    @Test
    fun `Mastered, Partial and Failed counts derive from cardResults and cannot disagree with it`() {
        val session = result(
            cardResults = listOf(
                cardResult(cardId = "card-1", state = FlashcardStudyProgressState.Mastered, attemptsUsed = 1),
                cardResult(cardId = "card-2", state = FlashcardStudyProgressState.Mastered, attemptsUsed = 2),
                cardResult(cardId = "card-3", state = FlashcardStudyProgressState.Partial, attemptsUsed = 3),
                cardResult(cardId = "card-4", state = FlashcardStudyProgressState.Failed, attemptsUsed = 3),
            ),
        )

        session.masteredCount shouldBe 2
        session.partialCount shouldBe 1
        session.failedCount shouldBe 1
        session.studiedCount shouldBe 4
    }

    @Test
    fun `empty cardResults reports every count as zero`() {
        val session = result(cardResults = emptyList())

        session.studiedCount shouldBe 0
        session.masteredCount shouldBe 0
        session.partialCount shouldBe 0
        session.failedCount shouldBe 0
    }
}
