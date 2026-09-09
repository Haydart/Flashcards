package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.CardProgressEntry
import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.ScoringState
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.SessionXpResult
import com.rossomak.flashcards.core.domain.model.SubcategoryProgress
import com.rossomak.flashcards.core.domain.repository.FakeCardProgressRepository
import com.rossomak.flashcards.core.domain.repository.FakeScoringStateRepository
import com.rossomak.flashcards.core.domain.repository.FakeSessionSubmissionRepository
import io.kotest.matchers.shouldBe
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SubmitStudySessionUseCaseTest {

    private val sessionSubmissionRepository = FakeSessionSubmissionRepository()
    private val cardProgressRepository = FakeCardProgressRepository()
    private val scoringStateRepository = FakeScoringStateRepository()

    private fun createUseCase(): SubmitStudySessionUseCase = SubmitStudySessionUseCase(
        cardProgressRepository,
        scoringStateRepository,
        CalculateSessionXpUseCase(),
        sessionSubmissionRepository,
    )

    private fun ratedSessionResult(
        subcategoryId: String = "sub-1",
        cardResults: List<FlashcardResult.Rated>,
    ): SessionResult.Rated = SessionResult.Rated(
        id = "session-1",
        startedAt = Instant.parse("2026-09-08T10:00:00Z"),
        durationSeconds = 60,
        abandoned = false,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf(subcategoryId),
        subcategoryNames = listOf("Subcategory"),
        cardResults = cardResults,
        studyDate = "2026-09-08",
        dailyGoalMinutes = 20,
        studyDateUtcOffsetMinutes = 0,
    )

    private fun fastSessionResult(
        subcategoryId: String = "sub-1",
        cardResults: List<FlashcardResult.Fast>,
    ): SessionResult.Fast = SessionResult.Fast(
        id = "session-1",
        startedAt = Instant.parse("2026-09-08T10:00:00Z"),
        durationSeconds = 60,
        abandoned = false,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf(subcategoryId),
        subcategoryNames = listOf("Subcategory"),
        cardResults = cardResults,
        studyDate = "2026-09-08",
        dailyGoalMinutes = 20,
        studyDateUtcOffsetMinutes = 0,
    )

    private fun ratedEntry(
        cardId: String = "card-1",
        subcategoryId: String = "sub-1",
        state: FlashcardStudyProgressState,
    ): FlashcardResult.Rated = FlashcardResult.Rated(
        cardId = cardId,
        subcategoryId = subcategoryId,
        state = state,
        attemptsUsed = 1,
        wasPreviouslyMastered = false,
    )

    private fun fastEntry(cardId: String = "card-1", subcategoryId: String = "sub-1"): FlashcardResult.Fast =
        FlashcardResult.Fast(cardId = cardId, subcategoryId = subcategoryId, state = FlashcardStudyProgressState.Seen)

    private suspend fun SubmitStudySessionUseCase.invokeAndCapturePreview(sessionResult: SessionResult): Result<SessionXpResult> {
        var preview: Result<SessionXpResult>? = null
        invoke(sessionResult) { preview = it }
        return requireNotNull(preview) { "onPreviewReady was never called" }
    }

    @Test
    fun `hands the exact session result to the submission repository`() = runTest {
        val session = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        createUseCase().invokeAndCapturePreview(session)

        sessionSubmissionRepository.submittedSessionResults shouldBe listOf(session)
    }

    @Test
    fun `a card with no prior entry counts as new, a card with one does not`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-2" to priorEntry())),
        )
        val session = ratedSessionResult(
            cardResults = listOf(
                ratedEntry(cardId = "card-1", state = FlashcardStudyProgressState.Mastered),
                ratedEntry(cardId = "card-2", state = FlashcardStudyProgressState.Partial),
            ),
        )

        val preview = createUseCase().invokeAndCapturePreview(session)

        preview.getOrThrow().newCardsStudied shouldBe 1
    }

    @Test
    fun `new cards are counted per Subcategory across the whole session`() = runTest {
        val session = SessionResult.Rated(
            id = "session-1",
            startedAt = Instant.parse("2026-09-08T10:00:00Z"),
            durationSeconds = 60,
            abandoned = false,
            categoryId = "cat-1",
            categoryName = "Category",
            subcategoryIds = listOf("sub-1", "sub-2"),
            subcategoryNames = listOf("Subcategory 1", "Subcategory 2"),
            cardResults = listOf(
                ratedEntry(cardId = "card-1", subcategoryId = "sub-1", state = FlashcardStudyProgressState.Mastered),
                ratedEntry(cardId = "card-2", subcategoryId = "sub-2", state = FlashcardStudyProgressState.Failed),
            ),
            studyDate = "2026-09-08",
            dailyGoalMinutes = 20,
            studyDateUtcOffsetMinutes = 0,
        )

        val preview = createUseCase().invokeAndCapturePreview(session)

        preview.getOrThrow().newCardsStudied shouldBe 2
    }

    @Test
    fun `a Fast card with no prior entry counts toward new-cards-studied exactly like a Rated one`() = runTest {
        val session = fastSessionResult(cardResults = listOf(fastEntry(cardId = "card-1")))

        val preview = createUseCase().invokeAndCapturePreview(session)

        preview.getOrThrow().newCardsStudied shouldBe 1
    }

    @Test
    fun `a failed prior-progress read fails the preview but the session is still submitted`() = runTest {
        val error = IllegalStateException("firestore down")
        cardProgressRepository.resultToReturn = Result.failure(error)
        val session = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        val preview = createUseCase().invokeAndCapturePreview(session)

        preview.isFailure shouldBe true
        preview.exceptionOrNull() shouldBe error
        sessionSubmissionRepository.submittedSessionResults shouldBe listOf(session)
    }

    @Test
    fun `a failed scoring-state read fails the preview but the session is still submitted`() = runTest {
        val error = IllegalStateException("firestore down")
        scoringStateRepository.resultToReturn = Result.failure(error)
        val session = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        val preview = createUseCase().invokeAndCapturePreview(session)

        preview.isFailure shouldBe true
        preview.exceptionOrNull() shouldBe error
        sessionSubmissionRepository.submittedSessionResults shouldBe listOf(session)
    }

    @Test
    fun `a missing scoring-state document starts the preview calculation from defaults`() = runTest {
        scoringStateRepository.resultToReturn = Result.success(null)
        val session = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        val preview = createUseCase().invokeAndCapturePreview(session).getOrThrow()

        preview.newScoringState.xp shouldBe preview.breakdown.xpTotal.toLong()
    }

    @Test
    fun `the preview carries the calculated xp breakdown starting from the prior scoring state`() = runTest {
        val priorXp = 40L
        scoringStateRepository.resultToReturn = Result.success(ScoringState(xp = priorXp))
        val session = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        val preview = createUseCase().invokeAndCapturePreview(session).getOrThrow()

        preview.newScoringState.xp shouldBe priorXp + preview.breakdown.xpTotal
    }

    @Test
    fun `a failed submission does not affect the already-computed preview`() = runTest {
        sessionSubmissionRepository.resultToReturn = Result.failure(IllegalStateException("unauthenticated"))
        val session = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        val preview = createUseCase().invokeAndCapturePreview(session)

        preview.isSuccess shouldBe true
        preview.getOrThrow().breakdown.mastered shouldBe 100
    }

    private fun priorEntry(): CardProgressEntry = CardProgressEntry(
        state = FlashcardStudyProgressState.Partial,
        firstStudiedAt = Instant.parse("2026-01-01T00:00:00Z"),
        masteredAt = null,
    )
}
