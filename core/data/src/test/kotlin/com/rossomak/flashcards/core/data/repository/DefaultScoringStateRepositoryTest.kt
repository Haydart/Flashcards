package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.model.ScoringStateDto
import com.rossomak.flashcards.core.data.source.ScoringStateRemoteDataSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultScoringStateRepositoryTest {

    private val remoteDataSource: ScoringStateRemoteDataSource = mockk()

    private fun createRepository(): DefaultScoringStateRepository = DefaultScoringStateRepository(remoteDataSource)

    @Test
    fun `getScoringState maps the dto to domain`() = runTest {
        val xp = 620L
        val level = 2
        val xpIntoCurrentLevel = 100L
        val currentStreak = 3
        val bestStreak = 5
        val lastStudyDate = "2026-09-06"
        val goalMetDate = "2026-09-06"
        val dto = ScoringStateDto(
            xp = xp,
            level = level,
            xpIntoCurrentLevel = xpIntoCurrentLevel,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            lastStudyDate = lastStudyDate,
            goalMetDate = goalMetDate,
        )
        coEvery { remoteDataSource.getScoringState() } returns dto

        val result = createRepository().getScoringState()

        result.isSuccess shouldBe true
        val state = result.getOrThrow()
        state?.xp shouldBe xp
        state?.level shouldBe level
        state?.xpIntoCurrentLevel shouldBe xpIntoCurrentLevel
        state?.currentStreak shouldBe currentStreak
        state?.bestStreak shouldBe bestStreak
        state?.lastStudyDate shouldBe lastStudyDate
        state?.goalMetDate shouldBe goalMetDate
        coVerify(exactly = 1) { remoteDataSource.getScoringState() }
    }

    @Test
    fun `getScoringState returns success with null for an absent document`() = runTest {
        coEvery { remoteDataSource.getScoringState() } returns null

        val result = createRepository().getScoringState()

        result.isSuccess shouldBe true
        result.getOrThrow() shouldBe null
        coVerify(exactly = 1) { remoteDataSource.getScoringState() }
    }

    @Test
    fun `getScoringState wraps a data source failure in a failure result`() = runTest {
        val error = IllegalStateException("firestore down")
        coEvery { remoteDataSource.getScoringState() } throws error

        val result = createRepository().getScoringState()

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { remoteDataSource.getScoringState() }
    }

    @Test
    fun `getScoringState rethrows cancellation instead of wrapping it`() = runTest {
        coEvery { remoteDataSource.getScoringState() } throws CancellationException("cancelled")

        val thrown = runCatching { createRepository().getScoringState() }.exceptionOrNull()

        (thrown is CancellationException) shouldBe true
        coVerify(exactly = 1) { remoteDataSource.getScoringState() }
    }
}
