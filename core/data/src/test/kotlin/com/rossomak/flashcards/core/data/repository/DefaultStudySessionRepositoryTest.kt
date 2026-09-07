package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.source.StudySessionRemoteDataSource
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.SessionLedgerEntry
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.StudyMode
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

    private fun sessionResult(): SessionResult = SessionResult(
        id = "session-1",
        mode = StudyMode.Rated,
        startedAt = Instant.parse("2026-09-06T10:00:00Z"),
        durationSeconds = 60,
        abandoned = false,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1"),
        subcategoryNames = listOf("Subcategory"),
        ledger = listOf(
            SessionLedgerEntry(
                cardId = "card-1",
                subcategoryId = "sub-1",
                state = FlashcardStudyProgressState.Mastered,
                attemptsUsed = 1,
                wasPreviouslyMastered = false,
            ),
        ),
    )

    @Test
    fun `commitSession delegates to the data source and returns success without awaiting it`() = runTest {
        val result = sessionResult()
        every { remoteDataSource.commitSession(result, any()) } just Runs

        val outcome = createRepository().commitSession(result)

        outcome.isSuccess shouldBe true
        verify(exactly = 1) { remoteDataSource.commitSession(result, any()) }
    }

    @Test
    fun `wraps a synchronous data source failure in a failure result`() = runTest {
        val result = sessionResult()
        val error = IllegalStateException("No authenticated user")
        every { remoteDataSource.commitSession(result, any()) } throws error

        val outcome = createRepository().commitSession(result)

        outcome.isFailure shouldBe true
        outcome.exceptionOrNull() shouldBe error
        verify(exactly = 1) { remoteDataSource.commitSession(result, any()) }
    }

    @Test
    fun `rethrows cancellation instead of wrapping it`() = runTest {
        val result = sessionResult()
        every { remoteDataSource.commitSession(result, any()) } throws CancellationException("cancelled")

        val thrown = runCatching { createRepository().commitSession(result) }.exceptionOrNull()

        (thrown is CancellationException) shouldBe true
        verify(exactly = 1) { remoteDataSource.commitSession(result, any()) }
    }

    @Test
    fun `forwards onRejected to the data source unchanged`() = runTest {
        val result = sessionResult()
        var reportedByDataSource: ((Throwable) -> Unit)? = null
        every { remoteDataSource.commitSession(result, any()) } answers {
            reportedByDataSource = secondArg()
        }
        val error = IllegalStateException("permission denied")
        var reportedToCaller: Throwable? = null

        createRepository().commitSession(result) { rejection -> reportedToCaller = rejection }
        reportedByDataSource?.invoke(error)

        reportedToCaller shouldBe error
    }
}
