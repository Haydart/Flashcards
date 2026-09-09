package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.ScoringState
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.XpConfig
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Spec 05 ticket 02's primary seam: pure, no dispatchers, no repositories. Every expected value below
 * is composed from [CONFIG]'s own rates rather than a repeated literal, so a tuning change only ever
 * touches [CONFIG].
 */
class CalculateSessionXpUseCaseTest {

    private val useCase = CalculateSessionXpUseCase()

    private fun ratedResult(
        cardResults: List<FlashcardResult.Rated>,
        abandoned: Boolean = false,
        durationSeconds: Int = 0,
        config: XpConfig = CONFIG,
    ): SessionResult.Rated = SessionResult.Rated(
        id = "session-1",
        startedAt = Instant.parse("2026-09-06T10:00:00Z"),
        durationSeconds = durationSeconds,
        abandoned = abandoned,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1"),
        subcategoryNames = listOf("Subcategory"),
        cardResults = cardResults,
        studyDate = "2026-09-06",
        dailyGoalMinutes = 20,
        xpConfig = config,
    )

    private fun fastResult(
        cardResults: List<FlashcardResult.Fast>,
        abandoned: Boolean = false,
        durationSeconds: Int = 0,
        config: XpConfig = CONFIG,
    ): SessionResult.Fast = SessionResult.Fast(
        id = "session-1",
        startedAt = Instant.parse("2026-09-06T10:00:00Z"),
        durationSeconds = durationSeconds,
        abandoned = abandoned,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1"),
        subcategoryNames = listOf("Subcategory"),
        cardResults = cardResults,
        studyDate = "2026-09-06",
        dailyGoalMinutes = 20,
        xpConfig = config,
    )

    private fun ratedEntry(
        cardId: String = "card-1",
        state: FlashcardStudyProgressState,
        wasPreviouslyMastered: Boolean = false,
    ): FlashcardResult.Rated = FlashcardResult.Rated(
        cardId = cardId,
        subcategoryId = "sub-1",
        state = state,
        attemptsUsed = 1,
        wasPreviouslyMastered = wasPreviouslyMastered,
    )

    private fun fastEntry(cardId: String = "card-1"): FlashcardResult.Fast =
        FlashcardResult.Fast(cardId = cardId, subcategoryId = "sub-1", state = FlashcardStudyProgressState.Seen)

    private suspend fun calculate(
        sessionResult: SessionResult,
        newCardsStudied: Int = 0,
        currentState: ScoringState = ScoringState(),
    ) = useCase(CalculateSessionXpUseCase.Params(sessionResult, newCardsStudied, currentState))

    @Test
    fun `a first-time card earns the new-card award`() = runTest {
        val xpResult = calculate(ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Failed))), newCardsStudied = 1)

        xpResult.breakdown.newCards shouldBe CONFIG.newCardStudied
    }

    @Test
    fun `the same card in a later session earns no further new-card award`() = runTest {
        val xpResult = calculate(ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Failed))), newCardsStudied = 0)

        xpResult.breakdown.newCards shouldBe 0
    }

    @Test
    fun `a card ending Mastered earns the mastered award`() = runTest {
        val xpResult = calculate(ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered))))

        xpResult.breakdown.mastered shouldBe CONFIG.cardMastered
    }

    @Test
    fun `a card ending Partial earns the partial award`() = runTest {
        val xpResult = calculate(ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Partial))))

        xpResult.breakdown.partial shouldBe CONFIG.cardPartial
    }

    @Test
    fun `a previously mastered card ending Mastered again earns the defense bonus instead of the mastered award`() = runTest {
        val xpResult = calculate(
            ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered, wasPreviouslyMastered = true))),
        )

        xpResult.breakdown.mastered shouldBe 0
        xpResult.breakdown.masteryDefenseBonus shouldBe CONFIG.masteryDefended
    }

    @Test
    fun `a previously mastered card ending Failed earns the demastered penalty`() = runTest {
        val xpResult = calculate(
            ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Failed, wasPreviouslyMastered = true))),
        )

        xpResult.breakdown.demastered shouldBe CONFIG.cardDemastered
    }

    @Test
    fun `a previously mastered card ending Partial earns neither the defense bonus nor the loss`() = runTest {
        val xpResult = calculate(
            ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Partial, wasPreviouslyMastered = true))),
        )

        xpResult.breakdown.masteryDefenseBonus shouldBe 0
        xpResult.breakdown.demastered shouldBe 0
        xpResult.breakdown.partial shouldBe CONFIG.cardPartial
    }

    @Test
    fun `a never-previously-mastered card ending Failed earns and costs nothing`() = runTest {
        // abandoned, so the completion bonus does not mask an otherwise-nonzero total.
        val xpResult = calculate(ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Failed)), abandoned = true))

        xpResult.breakdown.xpTotal shouldBe 0
    }

    @Test
    fun `a Rated result earns mastery, partial and defense awards as its ledger dictates, combined`() = runTest {
        val xpResult = calculate(
            ratedResult(
                cardResults = listOf(
                    ratedEntry(cardId = "card-1", state = FlashcardStudyProgressState.Mastered),
                    ratedEntry(cardId = "card-2", state = FlashcardStudyProgressState.Mastered, wasPreviouslyMastered = true),
                    ratedEntry(cardId = "card-3", state = FlashcardStudyProgressState.Partial),
                    ratedEntry(cardId = "card-4", state = FlashcardStudyProgressState.Failed, wasPreviouslyMastered = true),
                    ratedEntry(cardId = "card-5", state = FlashcardStudyProgressState.Failed),
                ),
            ),
        )

        // card-1 is a fresh mastery (mastered); card-2 is a defended one (masteryDefenseBonus, not mastered again).
        xpResult.breakdown.mastered shouldBe CONFIG.cardMastered
        xpResult.breakdown.masteryDefenseBonus shouldBe CONFIG.masteryDefended
        xpResult.breakdown.partial shouldBe CONFIG.cardPartial
        xpResult.breakdown.demastered shouldBe CONFIG.cardDemastered
    }

    @Test
    fun `a Fast result earns new-card, time and completion awards and no mastery awards`() = runTest {
        val xpResult = calculate(fastResult(cardResults = listOf(fastEntry()), durationSeconds = 120), newCardsStudied = 1)

        xpResult.breakdown.newCards shouldBe CONFIG.newCardStudied
        xpResult.breakdown.timeStudied shouldBe 2 * CONFIG.minuteStudied
        xpResult.breakdown.sessionCompletionBonus shouldBe CONFIG.sessionCompleted
        xpResult.breakdown.mastered shouldBe 0
        xpResult.breakdown.partial shouldBe 0
        xpResult.breakdown.masteryDefenseBonus shouldBe 0
        xpResult.breakdown.demastered shouldBe 0
    }

    @Test
    fun `time studied earns a flat rate per whole minute, both modes`() = runTest {
        val xpResult = calculate(ratedResult(cardResults = emptyList(), durationSeconds = 185))

        xpResult.breakdown.timeStudied shouldBe 3 * CONFIG.minuteStudied
    }

    @Test
    fun `a finished session earns the completion bonus`() = runTest {
        val xpResult = calculate(ratedResult(cardResults = emptyList(), abandoned = false))

        xpResult.breakdown.sessionCompletionBonus shouldBe CONFIG.sessionCompleted
    }

    @Test
    fun `an abandoned session omits the completion award but keeps the rest`() = runTest {
        val xpResult = calculate(
            ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)), abandoned = true, durationSeconds = 60),
            newCardsStudied = 1,
        )

        xpResult.breakdown.sessionCompletionBonus shouldBe 0
        xpResult.breakdown.newCards shouldBe CONFIG.newCardStudied
        xpResult.breakdown.mastered shouldBe CONFIG.cardMastered
        xpResult.breakdown.timeStudied shouldBe CONFIG.minuteStudied
    }

    @Test
    fun `lines worth zero are absent from the breakdown`() = runTest {
        val xpResult = calculate(ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Failed))))

        xpResult.breakdown.mastered shouldBe 0
        xpResult.breakdown.partial shouldBe 0
        xpResult.breakdown.masteryDefenseBonus shouldBe 0
        xpResult.breakdown.demastered shouldBe 0
        xpResult.breakdown.dailyGoalBonus shouldBe 0
        xpResult.breakdown.streakBonus shouldBe 0
    }

    @Test
    fun `the same result scored under two different configurations yields two different totals`() = runTest {
        val result = ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)), config = CONFIG)
        val otherConfig = CONFIG.copy(cardMastered = CONFIG.cardMastered * 2)
        val resultUnderOtherConfig = result.copy(xpConfig = otherConfig)

        val first = calculate(result)
        val second = calculate(resultUnderOtherConfig)

        first.breakdown.xpTotal shouldNotBe second.breakdown.xpTotal
    }

    @Test
    fun `a positive delta climbs the level and reports the crossing`() = runTest {
        // level 1's threshold is ceil(1000 * 1^2 / 1000) * 1000 = 1000, so a 1200-point session crosses it once.
        // abandoned, and no other award in play, so the whole delta is exactly cardMastered.
        val xpResult = calculate(
            ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)), abandoned = true),
            currentState = ScoringState(xp = 0, level = 1, xpIntoCurrentLevel = (1200 - CONFIG.cardMastered).toLong()),
        )

        xpResult.newScoringState.level shouldBe 2
        xpResult.levelsCrossed shouldBe listOf(2)
    }

    @Test
    fun `crossing several level boundaries at once reports them in order`() = runTest {
        // level 1's threshold is 1000, level 2's is ceil(1000 * 2^2 / 1000) * 1000 = 4000.
        val xpResult = calculate(
            ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)), abandoned = true),
            currentState = ScoringState(xp = 0, level = 1, xpIntoCurrentLevel = (5100 - CONFIG.cardMastered).toLong()),
        )

        xpResult.newScoringState.level shouldBe 3
        xpResult.levelsCrossed shouldBe listOf(2, 3)
    }

    @Test
    fun `no points burst is awarded on levelling up, the leftover carries into the new level`() = runTest {
        val xpResult = calculate(
            ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)), abandoned = true),
            currentState = ScoringState(xp = 0, level = 1, xpIntoCurrentLevel = (1050 - CONFIG.cardMastered).toLong()),
        )

        xpResult.newScoringState.level shouldBe 2
        xpResult.newScoringState.xpIntoCurrentLevel shouldBe 50
    }

    @Test
    fun `a loss large enough to drop below the level floor clamps, leaves the level unchanged`() = runTest {
        val xpResult = calculate(
            ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Failed, wasPreviouslyMastered = true)), abandoned = true),
            currentState = ScoringState(xp = 500, level = 2, xpIntoCurrentLevel = 30),
        )

        // demastered is -80 (CONFIG), which would push xpIntoCurrentLevel to -50 unclamped.
        xpResult.newScoringState.level shouldBe 2
        xpResult.newScoringState.xpIntoCurrentLevel shouldBe 0
        xpResult.newScoringState.xp shouldBe 470
    }

    @Test
    fun `a loss smaller than the points already in the level is applied in full, unclamped`() = runTest {
        val xpResult = calculate(
            ratedResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Failed, wasPreviouslyMastered = true)), abandoned = true),
            currentState = ScoringState(xp = 500, level = 2, xpIntoCurrentLevel = 100),
        )

        xpResult.newScoringState.xpIntoCurrentLevel shouldBe 100 + CONFIG.cardDemastered
        xpResult.newScoringState.xp shouldBe 500 + CONFIG.cardDemastered
        xpResult.newScoringState.level shouldBe 2
    }

    @Test
    fun `every rate is read from the snapshot, none is a literal — verified by an unusual configuration`() = runTest {
        val unusualConfig = XpConfig(
            newCardStudied = 3,
            cardMastered = 7,
            cardPartial = 11,
            masteryDefended = 13,
            cardDemastered = -17,
            sessionCompleted = 19,
            minuteStudied = 23,
        )
        val xpResult = calculate(
            ratedResult(
                cardResults = listOf(
                    ratedEntry(cardId = "card-1", state = FlashcardStudyProgressState.Mastered),
                    ratedEntry(cardId = "card-2", state = FlashcardStudyProgressState.Mastered, wasPreviouslyMastered = true),
                    ratedEntry(cardId = "card-3", state = FlashcardStudyProgressState.Partial),
                    ratedEntry(cardId = "card-4", state = FlashcardStudyProgressState.Failed, wasPreviouslyMastered = true),
                ),
                durationSeconds = 60,
                config = unusualConfig,
            ),
            newCardsStudied = 2,
        )

        // card-1 is a fresh mastery (mastered); card-2 is a defended one (masteryDefenseBonus, not mastered again).
        xpResult.breakdown.newCards shouldBe 6
        xpResult.breakdown.mastered shouldBe 7
        xpResult.breakdown.partial shouldBe 11
        xpResult.breakdown.masteryDefenseBonus shouldBe 13
        xpResult.breakdown.demastered shouldBe -17
        xpResult.breakdown.sessionCompletionBonus shouldBe 19
        xpResult.breakdown.timeStudied shouldBe 23
    }

    private companion object {
        val CONFIG = XpConfig(
            newCardStudied = 10,
            cardMastered = 100,
            cardPartial = 25,
            masteryDefended = 50,
            cardDemastered = -80,
            sessionCompleted = 500,
            dailyGoalMet = 1000,
            streakPerDay = 250,
            streakMaxPerDay = 2500,
            minuteStudied = 10,
            levelCurveBase = 1000.0,
            levelCurveExponent = 2.0,
        )
    }
}
