package com.rossomak.flashcards.core.data.repository

import android.util.Log
import com.rossomak.flashcards.core.data.SessionSubmissionDrainScheduler
import com.rossomak.flashcards.core.data.model.PendingSessionSubmissionDto
import com.rossomak.flashcards.core.data.model.PendingSessionSubmissionMapper.toDto
import com.rossomak.flashcards.core.data.source.FakePendingSessionSubmissionLocalDataSource
import com.rossomak.flashcards.core.data.source.PendingSessionSubmissionLocalDataSource
import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.SessionResult
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class DefaultSessionSubmissionRepositoryTest {

    private val localDataSource = FakePendingSessionSubmissionLocalDataSource()
    private val drainScheduler: SessionSubmissionDrainScheduler = mockk(relaxed = true)

    private fun createRepository(): DefaultSessionSubmissionRepository =
        DefaultSessionSubmissionRepository(localDataSource, drainScheduler)

    @Before
    fun setUp() {
        // Debug logging and a local-write failure both go through android.util.Log, unavailable
        // outside instrumented/Robolectric tests — stub it rather than pull in either just for this.
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun sessionResult(): SessionResult.Rated = SessionResult.Rated(
        id = "session-1",
        startedAt = Instant.parse("2026-09-08T10:00:00Z"),
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
    )

    @Test
    fun `submitSession appends the mapped session to the local queue and schedules a drain`() = runTest {
        val session = sessionResult()

        val result = createRepository().submitSession(session)

        result.isSuccess shouldBe true
        localDataSource.listAll() shouldBe listOf(session.toDto())
        verify(exactly = 1) { drainScheduler.scheduleDrain() }
    }

    @Test
    fun `submitSession returns success once queued, independent of whether the drain has run yet`() = runTest {
        // scheduleDrain is relaxed/never actually runs a Worker in this test — the point under test
        // is that submitSession's own success does not wait on delivery, only on the local append.
        val result = createRepository().submitSession(sessionResult())

        result.isSuccess shouldBe true
    }

    @Test
    fun `a local append failure is caught, logged and returned as a failure Result, never thrown`() = runTest {
        val failingLocalDataSource = ThrowingPendingSessionSubmissionLocalDataSource()
        val repository = DefaultSessionSubmissionRepository(failingLocalDataSource, drainScheduler)

        val result = repository.submitSession(sessionResult())

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe failingLocalDataSource.thrownException
        verify(exactly = 0) { drainScheduler.scheduleDrain() }
    }
}

private class ThrowingPendingSessionSubmissionLocalDataSource : PendingSessionSubmissionLocalDataSource {

    val thrownException = IllegalStateException("disk full")

    override suspend fun append(pendingSessionSubmission: PendingSessionSubmissionDto): Unit = throw thrownException

    override suspend fun listAll(): List<PendingSessionSubmissionDto> = emptyList()

    override suspend fun remove(sessionId: String) = Unit
}
