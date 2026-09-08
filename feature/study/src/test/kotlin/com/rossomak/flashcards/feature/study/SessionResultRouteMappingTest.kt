package com.rossomak.flashcards.feature.study

import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.XpConfig
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.Test

/**
 * Not an encoding test (the spec is explicit that route-argument encoding is not what's being
 * asserted) — this only checks that flattening onto [StudySessionSummaryRoute] and reading it back
 * loses nothing.
 */
class SessionResultRouteMappingTest {

    private val ratedResult = SessionResult.Rated(
        id = "session-1",
        startedAt = Instant.parse("2026-09-06T10:00:00Z"),
        durationSeconds = 90,
        abandoned = true,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1", "sub-2"),
        subcategoryNames = listOf("Subcategory 1", "Subcategory 2"),
        cardResults = listOf(
            FlashcardResult.Rated(
                cardId = "card-1",
                subcategoryId = "sub-1",
                state = FlashcardStudyProgressState.Mastered,
                attemptsUsed = 1,
                wasPreviouslyMastered = false,
            ),
            FlashcardResult.Rated(
                cardId = "card-2",
                subcategoryId = "sub-2",
                state = FlashcardStudyProgressState.Partial,
                attemptsUsed = 2,
                wasPreviouslyMastered = true,
            ),
        ),
    )

    private val fastResult = SessionResult.Fast(
        id = "session-2",
        startedAt = Instant.parse("2026-09-06T10:00:00Z"),
        durationSeconds = 90,
        abandoned = true,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1", "sub-2"),
        subcategoryNames = listOf("Subcategory 1", "Subcategory 2"),
        cardResults = listOf(
            FlashcardResult.Fast(cardId = "card-1", subcategoryId = "sub-1", state = FlashcardStudyProgressState.Seen),
            FlashcardResult.Fast(cardId = "card-2", subcategoryId = "sub-2", state = FlashcardStudyProgressState.Seen),
        ),
    )

    @Test
    fun `a Rated SessionResult survives a round trip through StudySessionSummaryRoute unchanged`() {
        val roundTripped = ratedResult.toSummaryRoute().toSessionResult()

        roundTripped shouldBe ratedResult
    }

    @Test
    fun `empty cardResults survive the round trip as empty cardResults`() {
        val empty = ratedResult.copy(cardResults = emptyList())

        empty.toSummaryRoute().toSessionResult() shouldBe empty
    }

    @Test
    fun `a Fast SessionResult flattens attemptsUsed and wasPreviouslyMastered to null and round-trips unchanged`() {
        val route = fastResult.toSummaryRoute()

        route.cardAttemptsUsed shouldBe null
        route.cardWasPreviouslyMastered shouldBe null
        route.toSessionResult() shouldBe fastResult
    }

    @Test
    fun `a non-default xpConfig survives the round trip unchanged`() {
        val customConfig = XpConfig(newCardStudied = 1, cardMastered = 2, levelCurveBase = 3.0, levelCurveExponent = 4.0)
        val withCustomConfig = ratedResult.copy(xpConfig = customConfig)

        val route = withCustomConfig.toSummaryRoute()

        route.xpConfig shouldBe customConfig
        route.toSessionResult() shouldBe withCustomConfig
    }
}
