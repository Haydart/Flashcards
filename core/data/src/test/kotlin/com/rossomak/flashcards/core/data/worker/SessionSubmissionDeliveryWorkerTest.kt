package com.rossomak.flashcards.core.data.worker

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.rossomak.flashcards.core.data.model.PendingFlashcardResultDto
import com.rossomak.flashcards.core.data.model.PendingSessionSubmissionDto
import com.rossomak.flashcards.core.data.model.PendingXpConfigDto
import com.rossomak.flashcards.core.data.repository.RemoteSessionSubmissionRepository
import com.rossomak.flashcards.core.data.source.FakePendingSessionSubmissionLocalDataSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Exercises [SessionSubmissionDeliveryWorker.doWork] directly rather than via
 * [androidx.work.testing.TestListenableWorkerBuilder]: that harness needs
 * `ApplicationProvider.getApplicationContext()`, which requires an instrumented or Robolectric
 * environment, and this project's plain-JVM unit tests deliberately run without Robolectric
 * (TESTING.md). `doWork()` itself never touches the mocked `Context`/`WorkerParameters` constructor
 * arguments — only the injected repository and local data source — so direct instantiation exercises
 * the exact same logic.
 */
class SessionSubmissionDeliveryWorkerTest {

    private val remoteSessionSubmissionRepository: RemoteSessionSubmissionRepository = mockk()
    private val localDataSource = FakePendingSessionSubmissionLocalDataSource()

    private fun createWorker(): SessionSubmissionDeliveryWorker = SessionSubmissionDeliveryWorker(
        mockk<Context>(),
        mockk<WorkerParameters>(),
        remoteSessionSubmissionRepository,
        localDataSource,
    )

    private fun pendingSubmission(sessionId: String, startedAtEpochMillis: Long): PendingSessionSubmissionDto = PendingSessionSubmissionDto(
        id = sessionId,
        mode = "Rated",
        startedAtEpochMillis = startedAtEpochMillis,
        durationSeconds = 60,
        abandoned = false,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1"),
        subcategoryNames = listOf("Subcategory"),
        cardResults = listOf(
            PendingFlashcardResultDto(cardId = "card-1", subcategoryId = "sub-1", state = "Mastered", attemptsUsed = 1, wasPreviouslyMastered = false),
        ),
        xpConfig = PendingXpConfigDto(
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
            levelCurveExponent = 2.5,
        ),
    )

    @Test
    fun `doWork submits every pending entry oldest-first by session start time`() = runTest {
        // Seeded out of start-time order on purpose — the drain must sort, not trust append order.
        val second = pendingSubmission("session-2", startedAtEpochMillis = 2_000L)
        val first = pendingSubmission("session-1", startedAtEpochMillis = 1_000L)
        localDataSource.seed(second)
        localDataSource.seed(first)
        coEvery { remoteSessionSubmissionRepository.submitSession(any()) } returns kotlin.Result.success(Unit)

        createWorker().doWork()

        coVerifySequence {
            remoteSessionSubmissionRepository.submitSession(withArg { it.id shouldBe "session-1" })
            remoteSessionSubmissionRepository.submitSession(withArg { it.id shouldBe "session-2" })
        }
    }

    @Test
    fun `doWork clears every entry that was delivered successfully`() = runTest {
        localDataSource.seed(pendingSubmission("session-1", startedAtEpochMillis = 1_000L))
        localDataSource.seed(pendingSubmission("session-2", startedAtEpochMillis = 2_000L))
        coEvery { remoteSessionSubmissionRepository.submitSession(any()) } returns kotlin.Result.success(Unit)

        val result = createWorker().doWork()

        result shouldBe Result.success()
        localDataSource.listAll() shouldBe emptyList()
    }

    @Test
    fun `a failure partway through a multi-entry drain leaves the failed and later entries queued`() = runTest {
        val first = pendingSubmission("session-1", startedAtEpochMillis = 1_000L)
        val second = pendingSubmission("session-2", startedAtEpochMillis = 2_000L)
        val third = pendingSubmission("session-3", startedAtEpochMillis = 3_000L)
        localDataSource.seed(first)
        localDataSource.seed(second)
        localDataSource.seed(third)
        coEvery { remoteSessionSubmissionRepository.submitSession(match { it.id == "session-1" }) } returns kotlin.Result.success(Unit)
        coEvery { remoteSessionSubmissionRepository.submitSession(match { it.id == "session-2" }) } returns
            kotlin.Result.failure(IllegalStateException("network error"))

        val result = createWorker().doWork()

        result shouldBe Result.retry()
        localDataSource.listAll().map { it.id } shouldBe listOf("session-2", "session-3")
        coVerify(exactly = 0) { remoteSessionSubmissionRepository.submitSession(match { it.id == "session-3" }) }
    }

    @Test
    fun `a single-entry drain returns retry on failure without clearing the entry`() = runTest {
        val entry = pendingSubmission("session-1", startedAtEpochMillis = 1_000L)
        localDataSource.seed(entry)
        coEvery { remoteSessionSubmissionRepository.submitSession(any()) } returns kotlin.Result.failure(IllegalStateException("offline"))

        val result = createWorker().doWork()

        result shouldBe Result.retry()
        localDataSource.listAll() shouldBe listOf(entry)
    }

    @Test
    fun `a single-entry drain clears the pending record on success`() = runTest {
        val entry = pendingSubmission("session-1", startedAtEpochMillis = 1_000L)
        localDataSource.seed(entry)
        coEvery { remoteSessionSubmissionRepository.submitSession(any()) } returns kotlin.Result.success(Unit)

        val result = createWorker().doWork()

        result shouldBe Result.success()
        localDataSource.listAll() shouldBe emptyList()
    }

    @Test
    fun `an empty queue succeeds without submitting anything`() = runTest {
        val result = createWorker().doWork()

        result shouldBe Result.success()
        coVerify(exactly = 0) { remoteSessionSubmissionRepository.submitSession(any()) }
    }
}
