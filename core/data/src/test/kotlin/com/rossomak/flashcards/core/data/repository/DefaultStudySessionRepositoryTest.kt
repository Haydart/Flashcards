package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.source.StudySessionRemoteDataSource
import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.ProgressSummaryWrite
import com.rossomak.flashcards.core.domain.model.ScoringState
import com.rossomak.flashcards.core.domain.model.SessionCommit
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.XpBreakdown
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultStudySessionRepositoryTest {

    private val remoteDataSource: StudySessionRemoteDataSource = mockk()

    private fun createRepository(): DefaultStudySessionRepository = DefaultStudySessionRepository(remoteDataSource)

    private fun sessionCommit(): SessionCommit = SessionCommit(
        sessionResult = SessionResult.Rated(
            id = "session-1",
            startedAt = Instant.parse("2026-09-06T10:00:00Z"),
            durationSeconds = 60,
            abandoned = false,
            categoryId = "cat-1",
            categoryName = "Category",
            subcategoryIds = listOf("sub-1"),
            subcategoryNames = listOf("Subcategory"),
            cardResults = listOf(
                FlashcardResult.Rated(
                    cardId = "card-1",
                    subcategoryId = "sub-1",
                    state = FlashcardStudyProgressState.Mastered,
                    attemptsUsed = 1,
                    wasPreviouslyMastered = false,
                ),
            ),
        ),
        newCardsStudied = 1,
        progressWrites = emptyList(),
        progressSummaryWrite = ProgressSummaryWrite(emptyMap()),
        xpBreakdown = XpBreakdown(),
        newScoringState = ScoringState(),
    )

    @Test
    fun `commitSession delegates to the data source and returns success without awaiting it`() = runTest {
        val commit = sessionCommit()
        every { remoteDataSource.commitSession(commit, any()) } just Runs

        val outcome = createRepository().commitSession(commit)

        outcome.isSuccess shouldBe true
        verify(exactly = 1) { remoteDataSource.commitSession(commit, any()) }
    }

    @Test
    fun `wraps a synchronous data source failure in a failure result`() = runTest {
        val commit = sessionCommit()
        val error = IllegalStateException("No authenticated user")
        every { remoteDataSource.commitSession(commit, any()) } throws error

        val outcome = createRepository().commitSession(commit)

        outcome.isFailure shouldBe true
        outcome.exceptionOrNull() shouldBe error
        verify(exactly = 1) { remoteDataSource.commitSession(commit, any()) }
    }

    @Test
    fun `rethrows cancellation instead of wrapping it`() = runTest {
        val commit = sessionCommit()
        every { remoteDataSource.commitSession(commit, any()) } throws CancellationException("cancelled")

        val thrown = runCatching { createRepository().commitSession(commit) }.exceptionOrNull()

        (thrown is CancellationException) shouldBe true
        verify(exactly = 1) { remoteDataSource.commitSession(commit, any()) }
    }

    @Test
    fun `forwards onRejected to the data source unchanged`() = runTest {
        val commit = sessionCommit()
        var reportedByDataSource: ((Throwable) -> Unit)? = null
        every { remoteDataSource.commitSession(commit, any()) } answers {
            reportedByDataSource = secondArg()
        }
        val error = IllegalStateException("permission denied")
        var reportedToCaller: Throwable? = null

        createRepository().commitSession(commit) { rejection -> reportedToCaller = rejection }
        reportedByDataSource?.invoke(error)

        reportedToCaller shouldBe error
    }
}
