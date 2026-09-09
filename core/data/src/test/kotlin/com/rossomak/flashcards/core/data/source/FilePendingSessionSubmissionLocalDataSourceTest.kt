package com.rossomak.flashcards.core.data.source

import android.content.Context
import android.util.Log
import com.rossomak.flashcards.core.data.model.PendingFlashcardResultDto
import com.rossomak.flashcards.core.data.model.PendingSessionSubmissionDto
import com.rossomak.flashcards.core.data.model.PendingXpConfigDto
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FilePendingSessionSubmissionLocalDataSourceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Before
    fun setUp() {
        // Debug logging and a corrupted queue file both go through android.util.Log, unavailable
        // outside instrumented/Robolectric tests — stub it rather than pull in either just for this.
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun createDataSource(): FilePendingSessionSubmissionLocalDataSource {
        val context: Context = mockk()
        every { context.filesDir } returns temporaryFolder.root
        return FilePendingSessionSubmissionLocalDataSource(context)
    }

    private fun pendingSubmission(sessionId: String, startedAtEpochMillis: Long = 0L): PendingSessionSubmissionDto = PendingSessionSubmissionDto(
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
        studyDate = "2026-09-08",
        dailyGoalMinutes = 20,
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
    fun `listAll on a fresh store with no file yet returns empty`() = runTest {
        val dataSource = createDataSource()

        dataSource.listAll() shouldBe emptyList()
    }

    @Test
    fun `append then listAll returns the appended entry`() = runTest {
        val dataSource = createDataSource()
        val submission = pendingSubmission("session-1")

        dataSource.append(submission)

        dataSource.listAll() shouldBe listOf(submission)
    }

    @Test
    fun `append twice preserves both entries in append order`() = runTest {
        val dataSource = createDataSource()
        val first = pendingSubmission("session-1")
        val second = pendingSubmission("session-2")

        dataSource.append(first)
        dataSource.append(second)

        dataSource.listAll() shouldBe listOf(first, second)
    }

    @Test
    fun `remove clears only the matching entry, keeping the rest`() = runTest {
        val dataSource = createDataSource()
        val first = pendingSubmission("session-1")
        val second = pendingSubmission("session-2")
        dataSource.append(first)
        dataSource.append(second)

        dataSource.remove("session-1")

        dataSource.listAll() shouldBe listOf(second)
    }

    @Test
    fun `remove of an unknown sessionId is a no-op`() = runTest {
        val dataSource = createDataSource()
        val submission = pendingSubmission("session-1")
        dataSource.append(submission)

        dataSource.remove("session-does-not-exist")

        dataSource.listAll() shouldBe listOf(submission)
    }

    @Test
    fun `a new instance reads back what a previous instance persisted, surviving a process restart`() = runTest {
        val context: Context = mockk()
        every { context.filesDir } returns temporaryFolder.root
        val submission = pendingSubmission("session-1")
        FilePendingSessionSubmissionLocalDataSource(context).append(submission)

        val reloaded = FilePendingSessionSubmissionLocalDataSource(context).listAll()

        reloaded shouldBe listOf(submission)
    }

    @Test
    fun `a corrupted queue file is treated as empty rather than crashing`() = runTest {
        val context: Context = mockk()
        every { context.filesDir } returns temporaryFolder.root
        File(temporaryFolder.root, "pending_session_submissions.json").writeText("{ not valid json ][")

        val dataSource = FilePendingSessionSubmissionLocalDataSource(context)

        dataSource.listAll() shouldBe emptyList()
    }

    @Test
    fun `a queue file that fails to read with an IOException is treated as empty rather than crashing`() = runTest {
        val context: Context = mockk()
        every { context.filesDir } returns temporaryFolder.root
        // A directory at the expected path exists (so file.exists() is true) but readText() throws
        // FileNotFoundException — an IOException — rather than returning content.
        File(temporaryFolder.root, "pending_session_submissions.json").mkdir()

        val dataSource = FilePendingSessionSubmissionLocalDataSource(context)

        dataSource.listAll() shouldBe emptyList()
    }

    @Test
    fun `append does not leave a stray temp file behind after its atomic write`() = runTest {
        val dataSource = createDataSource()

        dataSource.append(pendingSubmission("session-1"))

        File(temporaryFolder.root, "pending_session_submissions.json.tmp").exists() shouldBe false
    }
}
