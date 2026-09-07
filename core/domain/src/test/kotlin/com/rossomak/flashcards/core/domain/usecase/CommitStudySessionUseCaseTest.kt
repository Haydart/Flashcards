package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.SessionLedgerEntry
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.repository.FakeStudySessionRepository
import io.kotest.matchers.shouldBe
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CommitStudySessionUseCaseTest {

    private val studySessionRepository = FakeStudySessionRepository()

    private fun createUseCase(): CommitStudySessionUseCase = CommitStudySessionUseCase(studySessionRepository)

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
    fun `forwards the session result to the repository unchanged`() = runTest {
        val result = sessionResult()

        createUseCase().invoke(result)

        studySessionRepository.committedSessions shouldBe listOf(result)
    }

    @Test
    fun `returns the repository's result as-is on success`() = runTest {
        studySessionRepository.commitResultToReturn = Result.success(Unit)

        val outcome = createUseCase().invoke(sessionResult())

        outcome.isSuccess shouldBe true
    }

    @Test
    fun `returns the repository's result as-is on failure`() = runTest {
        val error = IllegalStateException("no authenticated user")
        studySessionRepository.commitResultToReturn = Result.failure(error)

        val outcome = createUseCase().invoke(sessionResult())

        outcome.isFailure shouldBe true
        outcome.exceptionOrNull() shouldBe error
    }

    @Test
    fun `forwards a later rejection through onRejected`() = runTest {
        val error = IllegalStateException("permission denied")
        studySessionRepository.rejectionToDeliver = error
        var reported: Throwable? = null

        createUseCase().invoke(sessionResult()) { rejection -> reported = rejection }

        reported shouldBe error
    }
}
