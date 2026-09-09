package com.rossomak.flashcards.core.data.repository

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** The documented defaults from spec 05's table, pinned so a change is deliberate, not accidental. */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultXpConfigRepositoryTest {

    private val repository = DefaultXpConfigRepository()

    @Test
    fun `getXpConfig returns the documented defaults for every value, including both curve parameters`() = runTest {
        val config = repository.getXpConfig().getOrThrow()

        config.newCardStudied shouldBe 10
        config.cardMastered shouldBe 100
        config.cardPartial shouldBe 25
        config.masteryDefended shouldBe 50
        config.cardDemastered shouldBe -80
        config.sessionCompleted shouldBe 500
        config.dailyGoalMet shouldBe 1000
        config.streakPerDay shouldBe 250
        config.streakMaxPerDay shouldBe 2500
        config.minuteStudied shouldBe 10
        config.levelCurveBase shouldBe 1000.0
        config.levelCurveExponent shouldBe 2.5
    }

    @Test
    fun `getXpConfig always succeeds`() = runTest {
        repository.getXpConfig().isSuccess shouldBe true
    }
}
