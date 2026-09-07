package com.rossomak.flashcards.feature.study

import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.StudyMode
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.Test

/**
 * Not an encoding test (the spec is explicit that route-argument encoding is not what's being
 * asserted) — this only checks that flattening onto [StudySessionSummaryRoute] and reading it back
 * loses nothing.
 */
class SessionResultRouteMappingTest {

    private val result = SessionResult(
        id = "session-1",
        mode = StudyMode.Rated,
        startedAt = Instant.parse("2026-09-06T10:00:00Z"),
        durationSeconds = 90,
        abandoned = true,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1", "sub-2"),
        subcategoryNames = listOf("Subcategory 1", "Subcategory 2"),
        cardResults = listOf(
            FlashcardResult(
                cardId = "card-1",
                subcategoryId = "sub-1",
                state = FlashcardStudyProgressState.Mastered,
                attemptsUsed = 1,
                wasPreviouslyMastered = false,
            ),
            FlashcardResult(
                cardId = "card-2",
                subcategoryId = "sub-2",
                state = FlashcardStudyProgressState.Partial,
                attemptsUsed = 2,
                wasPreviouslyMastered = true,
            ),
        ),
    )

    @Test
    fun `a SessionResult survives a round trip through StudySessionSummaryRoute unchanged`() {
        val roundTripped = result.toSummaryRoute().toSessionResult()

        roundTripped shouldBe result
    }

    @Test
    fun `empty cardResults survive the round trip as empty cardResults`() {
        val empty = result.copy(cardResults = emptyList())

        empty.toSummaryRoute().toSessionResult() shouldBe empty
    }

    @Test
    fun `a Fast SessionResult flattens attemptsUsed and wasPreviouslyMastered to null and reconstructs zero-value defaults`() {
        val fastResult = result.copy(
            mode = StudyMode.Fast,
            cardResults = result.cardResults.map { it.copy(attemptsUsed = 0, wasPreviouslyMastered = false) },
        )

        val route = fastResult.toSummaryRoute()
        route.cardAttemptsUsed shouldBe null
        route.cardWasPreviouslyMastered shouldBe null

        route.toSessionResult() shouldBe fastResult
    }
}
