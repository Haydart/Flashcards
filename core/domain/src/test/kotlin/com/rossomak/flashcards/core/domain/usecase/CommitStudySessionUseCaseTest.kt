package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.CardProgressEntry
import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.SubcategoryProgress
import com.rossomak.flashcards.core.domain.repository.FakeCardProgressRepository
import com.rossomak.flashcards.core.domain.repository.FakeStudySessionRepository
import io.kotest.matchers.shouldBe
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CommitStudySessionUseCaseTest {

    private val studySessionRepository = FakeStudySessionRepository()
    private val cardProgressRepository = FakeCardProgressRepository()

    private fun createUseCase(): CommitStudySessionUseCase =
        CommitStudySessionUseCase(studySessionRepository, cardProgressRepository)

    private fun sessionResult(
        mode: StudyMode = StudyMode.Rated,
        subcategoryId: String = "sub-1",
        cardResults: List<FlashcardResult>,
    ): SessionResult = SessionResult(
        id = "session-1",
        mode = mode,
        startedAt = Instant.parse("2026-09-06T10:00:00Z"),
        durationSeconds = 60,
        abandoned = false,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf(subcategoryId),
        subcategoryNames = listOf("Subcategory"),
        cardResults = cardResults,
    )

    private fun ratedEntry(
        cardId: String = "card-1",
        subcategoryId: String = "sub-1",
        state: FlashcardStudyProgressState,
    ): FlashcardResult = FlashcardResult(
        cardId = cardId,
        subcategoryId = subcategoryId,
        state = state,
        attemptsUsed = 1,
        wasPreviouslyMastered = false,
    )

    private fun fastEntry(cardId: String = "card-1", subcategoryId: String = "sub-1"): FlashcardResult =
        FlashcardResult(
            cardId = cardId,
            subcategoryId = subcategoryId,
            state = FlashcardStudyProgressState.Seen,
            attemptsUsed = 0,
            wasPreviouslyMastered = false,
        )

    private fun priorEntry(
        state: FlashcardStudyProgressState,
        firstStudiedAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        masteredAt: Instant? = null,
    ): CardProgressEntry = CardProgressEntry(state = state, firstStudiedAt = firstStudiedAt, masteredAt = masteredAt)

    private fun singleWrite() = studySessionRepository.committedSessionCommits.single().progressWrites.single()

    @Test
    fun `a card ending Mastered with no prior entry sets state Mastered and stamps the mastered timestamp`() = runTest {
        val result = sessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        createUseCase().invoke(result)

        val update = singleWrite().cards.getValue("card-1")
        update.state shouldBe FlashcardStudyProgressState.Mastered
        update.stampMastered shouldBe true
    }

    @Test
    fun `a card ending Partial on a previously mastered card leaves the state Mastered and writes no change`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-1" to priorEntry(FlashcardStudyProgressState.Mastered))),
        )
        val result = sessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Partial)))

        createUseCase().invoke(result)

        studySessionRepository.committedSessionCommits.single().progressWrites shouldBe emptyList()
    }

    @Test
    fun `a card ending Failed on a previously mastered card moves the state down and keeps the key`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-1" to priorEntry(FlashcardStudyProgressState.Mastered))),
        )
        val result = sessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Failed)))

        createUseCase().invoke(result)

        val update = singleWrite().cards.getValue("card-1")
        update.state shouldBe FlashcardStudyProgressState.Failed
        update.stampMastered shouldBe false
    }

    @Test
    fun `a Defended card, already mastered and ending Mastered again, writes no change`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-1" to priorEntry(FlashcardStudyProgressState.Mastered))),
        )
        val result = sessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        createUseCase().invoke(result)

        studySessionRepository.committedSessionCommits.single().progressWrites shouldBe emptyList()
    }

    @Test
    fun `a card with no prior entry stamps first-studied and counts as new, a card with one does neither`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-2" to priorEntry(FlashcardStudyProgressState.Partial))),
        )
        val result = sessionResult(
            cardResults = listOf(
                ratedEntry(cardId = "card-1", state = FlashcardStudyProgressState.Mastered),
                ratedEntry(cardId = "card-2", state = FlashcardStudyProgressState.Mastered),
            ),
        )

        createUseCase().invoke(result)

        val commit = studySessionRepository.committedSessionCommits.single()
        commit.newCardsStudied shouldBe 1
        val cards = commit.progressWrites.single().cards
        cards.getValue("card-1").stampFirstStudied shouldBe true
        cards.getValue("card-2").stampFirstStudied shouldBe false
    }

    @Test
    fun `a Fast result creates entries with state Seen only where none exist`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-2" to priorEntry(FlashcardStudyProgressState.Seen))),
        )
        val result = sessionResult(
            mode = StudyMode.Fast,
            cardResults = listOf(fastEntry(cardId = "card-1"), fastEntry(cardId = "card-2")),
        )

        createUseCase().invoke(result)

        val cards = singleWrite().cards
        cards.keys shouldBe setOf("card-1")
        cards.getValue("card-1").state shouldBe FlashcardStudyProgressState.Seen
    }

    @Test
    fun `a Fast result cannot downgrade an existing Mastered entry`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-1" to priorEntry(FlashcardStudyProgressState.Mastered))),
        )
        val result = sessionResult(mode = StudyMode.Fast, cardResults = listOf(fastEntry(cardId = "card-1")))

        createUseCase().invoke(result)

        studySessionRepository.committedSessionCommits.single().progressWrites shouldBe emptyList()
    }

    @Test
    fun `a Fast card with no prior entry counts toward new-cards-studied exactly like a Rated one`() = runTest {
        val result = sessionResult(mode = StudyMode.Fast, cardResults = listOf(fastEntry(cardId = "card-1")))

        createUseCase().invoke(result)

        studySessionRepository.committedSessionCommits.single().newCardsStudied shouldBe 1
    }

    @Test
    fun `a session spanning two Subcategories produces two progress writes with independent contents`() = runTest {
        val result = sessionResult(
            cardResults = listOf(
                ratedEntry(cardId = "card-1", subcategoryId = "sub-1", state = FlashcardStudyProgressState.Mastered),
                ratedEntry(cardId = "card-2", subcategoryId = "sub-2", state = FlashcardStudyProgressState.Failed),
            ),
        )

        createUseCase().invoke(result)

        val writes = studySessionRepository.committedSessionCommits.single().progressWrites
        writes.map { it.subcategoryId }.toSet() shouldBe setOf("sub-1", "sub-2")
        writes.single { it.subcategoryId == "sub-1" }.cards.keys shouldBe setOf("card-1")
        writes.single { it.subcategoryId == "sub-2" }.cards.keys shouldBe setOf("card-2")
    }

    @Test
    fun `an abandoned session writes only what its cardResults holds`() = runTest {
        val result = sessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Partial))).copy(abandoned = true)

        createUseCase().invoke(result)

        singleWrite().cards.keys shouldBe setOf("card-1")
    }

    @Test
    fun `a failed prior-progress read reports failure and writes nothing`() = runTest {
        val error = IllegalStateException("firestore down")
        cardProgressRepository.resultToReturn = Result.failure(error)
        val result = sessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        val outcome = createUseCase().invoke(result)

        outcome.isFailure shouldBe true
        outcome.exceptionOrNull() shouldBe error
        studySessionRepository.committedSessionCommits shouldBe emptyList()
    }

    @Test
    fun `returns the repository's result as-is on success`() = runTest {
        studySessionRepository.commitResultToReturn = Result.success(Unit)

        val outcome = createUseCase().invoke(sessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered))))

        outcome.isSuccess shouldBe true
    }

    @Test
    fun `returns the repository's result as-is on failure`() = runTest {
        val error = IllegalStateException("no authenticated user")
        studySessionRepository.commitResultToReturn = Result.failure(error)

        val outcome = createUseCase().invoke(sessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered))))

        outcome.isFailure shouldBe true
        outcome.exceptionOrNull() shouldBe error
    }

    @Test
    fun `forwards a later rejection through onRejected`() = runTest {
        val error = IllegalStateException("permission denied")
        studySessionRepository.rejectionToDeliver = error
        var reported: Throwable? = null

        createUseCase().invoke(sessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))) { rejection ->
            reported = rejection
        }

        reported shouldBe error
    }
}
