package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.CardProgressEntry
import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.ScoringState
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.SubcategoryProgress
import com.rossomak.flashcards.core.domain.model.SubcategoryProgressSummaryDelta
import com.rossomak.flashcards.core.domain.repository.FakeCardProgressRepository
import com.rossomak.flashcards.core.domain.repository.FakeScoringStateRepository
import com.rossomak.flashcards.core.domain.repository.FakeStudySessionRepository
import io.kotest.matchers.shouldBe
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CommitStudySessionUseCaseTest {

    private val studySessionRepository = FakeStudySessionRepository()
    private val cardProgressRepository = FakeCardProgressRepository()
    private val scoringStateRepository = FakeScoringStateRepository()

    private fun createUseCase(): CommitStudySessionUseCase = CommitStudySessionUseCase(
        studySessionRepository,
        cardProgressRepository,
        scoringStateRepository,
        CalculateSessionXpUseCase(),
    )

    private fun ratedSessionResult(
        subcategoryId: String = "sub-1",
        cardResults: List<FlashcardResult.Rated>,
    ): SessionResult.Rated = SessionResult.Rated(
        id = "session-1",
        startedAt = Instant.parse("2026-09-06T10:00:00Z"),
        durationSeconds = 60,
        abandoned = false,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf(subcategoryId),
        subcategoryNames = listOf("Subcategory"),
        cardResults = cardResults,
    )

    private fun fastSessionResult(
        subcategoryId: String = "sub-1",
        cardResults: List<FlashcardResult.Fast>,
    ): SessionResult.Fast = SessionResult.Fast(
        id = "session-1",
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
    ): FlashcardResult.Rated = FlashcardResult.Rated(
        cardId = cardId,
        subcategoryId = subcategoryId,
        state = state,
        attemptsUsed = 1,
        wasPreviouslyMastered = false,
    )

    private fun fastEntry(cardId: String = "card-1", subcategoryId: String = "sub-1"): FlashcardResult.Fast =
        FlashcardResult.Fast(
            cardId = cardId,
            subcategoryId = subcategoryId,
            state = FlashcardStudyProgressState.Seen,
        )

    private fun priorEntry(
        state: FlashcardStudyProgressState,
        firstStudiedAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        masteredAt: Instant? = null,
    ): CardProgressEntry = CardProgressEntry(state = state, firstStudiedAt = firstStudiedAt, masteredAt = masteredAt)

    private fun singleWrite() = studySessionRepository.committedSessionCommits.single().progressWrites.single()

    private fun singleSummaryDelta(subcategoryId: String = "sub-1") =
        studySessionRepository.committedSessionCommits.single().progressSummaryWrite.subcategoryDeltas.getValue(subcategoryId)

    @Test
    fun `a card ending Mastered with no prior entry sets state Mastered and stamps the mastered timestamp`() = runTest {
        val result = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

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
        val result = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Partial)))

        createUseCase().invoke(result)

        studySessionRepository.committedSessionCommits.single().progressWrites shouldBe emptyList()
    }

    @Test
    fun `a card ending Failed on a previously mastered card moves the state down and keeps the key`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-1" to priorEntry(FlashcardStudyProgressState.Mastered))),
        )
        val result = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Failed)))

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
        val result = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        createUseCase().invoke(result)

        studySessionRepository.committedSessionCommits.single().progressWrites shouldBe emptyList()
    }

    @Test
    fun `a card with no prior entry stamps first-studied and counts as new, a card with one does neither`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-2" to priorEntry(FlashcardStudyProgressState.Partial))),
        )
        val result = ratedSessionResult(
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
        val result = fastSessionResult(
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
        val result = fastSessionResult(cardResults = listOf(fastEntry(cardId = "card-1")))

        createUseCase().invoke(result)

        studySessionRepository.committedSessionCommits.single().progressWrites shouldBe emptyList()
    }

    @Test
    fun `a Fast card with no prior entry counts toward new-cards-studied exactly like a Rated one`() = runTest {
        val result = fastSessionResult(cardResults = listOf(fastEntry(cardId = "card-1")))

        createUseCase().invoke(result)

        studySessionRepository.committedSessionCommits.single().newCardsStudied shouldBe 1
    }

    @Test
    fun `a session spanning two Subcategories produces two progress writes with independent contents`() = runTest {
        val result = ratedSessionResult(
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
        val result = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Partial))).copy(abandoned = true)

        createUseCase().invoke(result)

        singleWrite().cards.keys shouldBe setOf("card-1")
    }

    @Test
    fun `a failed prior-progress read reports failure and writes nothing`() = runTest {
        val error = IllegalStateException("firestore down")
        cardProgressRepository.resultToReturn = Result.failure(error)
        val result = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        val xpResult = createUseCase().invoke(result)

        xpResult.isFailure shouldBe true
        xpResult.exceptionOrNull() shouldBe error
        studySessionRepository.committedSessionCommits shouldBe emptyList()
    }

    @Test
    fun `returns the repository's result as-is on success`() = runTest {
        studySessionRepository.commitResultToReturn = Result.success(Unit)

        val xpResult = createUseCase().invoke(ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered))))

        xpResult.isSuccess shouldBe true
    }

    @Test
    fun `a failed scoring-state read reports failure and writes nothing`() = runTest {
        val error = IllegalStateException("firestore down")
        scoringStateRepository.resultToReturn = Result.failure(error)
        val result = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        val xpResult = createUseCase().invoke(result)

        xpResult.isFailure shouldBe true
        xpResult.exceptionOrNull() shouldBe error
        studySessionRepository.committedSessionCommits shouldBe emptyList()
    }

    @Test
    fun `a missing scoring-state document starts the calculation from defaults`() = runTest {
        scoringStateRepository.resultToReturn = Result.success(null)
        val result = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        val xpResult = createUseCase().invoke(result).getOrThrow()

        xpResult.newScoringState.xp shouldBe xpResult.breakdown.xpTotal.toLong()
    }

    @Test
    fun `the commit carries the calculated xp breakdown and new scoring state`() = runTest {
        val priorXp = 40L
        scoringStateRepository.resultToReturn = Result.success(ScoringState(xp = priorXp))
        val result = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        val xpResult = createUseCase().invoke(result).getOrThrow()

        val commit = studySessionRepository.committedSessionCommits.single()
        commit.xpBreakdown shouldBe xpResult.breakdown
        commit.newScoringState shouldBe xpResult.newScoringState
        commit.newScoringState.xp shouldBe priorXp + xpResult.breakdown.xpTotal
    }

    @Test
    fun `returns the repository's result as-is on failure`() = runTest {
        val error = IllegalStateException("no authenticated user")
        studySessionRepository.commitResultToReturn = Result.failure(error)

        val xpResult = createUseCase().invoke(ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered))))

        xpResult.isFailure shouldBe true
        xpResult.exceptionOrNull() shouldBe error
    }

    @Test
    fun `forwards a later rejection through onRejected`() = runTest {
        val error = IllegalStateException("permission denied")
        studySessionRepository.rejectionToDeliver = error
        var reported: Throwable? = null

        createUseCase().invoke(ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))) { rejection ->
            reported = rejection
        }

        reported shouldBe error
    }

    @Test
    fun `a newly mastered card increments the topic's mastered count`() = runTest {
        val result = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        createUseCase().invoke(result)

        singleSummaryDelta() shouldBe SubcategoryProgressSummaryDelta(masteredDelta = 1, studiedDelta = 1)
    }

    @Test
    fun `a de-mastered card decrements the mastered count and leaves the studied count untouched`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-1" to priorEntry(FlashcardStudyProgressState.Mastered))),
        )
        val result = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Failed)))

        createUseCase().invoke(result)

        singleSummaryDelta() shouldBe SubcategoryProgressSummaryDelta(masteredDelta = -1, studiedDelta = 0)
    }

    @Test
    fun `a Defended card produces no entry in the summary write`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-1" to priorEntry(FlashcardStudyProgressState.Mastered))),
        )
        val result = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Mastered)))

        createUseCase().invoke(result)

        studySessionRepository.committedSessionCommits.single().progressSummaryWrite.subcategoryDeltas shouldBe emptyMap()
    }

    @Test
    fun `a card ending Partial moves neither count`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-1" to priorEntry(FlashcardStudyProgressState.Failed))),
        )
        val result = ratedSessionResult(cardResults = listOf(ratedEntry(state = FlashcardStudyProgressState.Partial)))

        createUseCase().invoke(result)

        studySessionRepository.committedSessionCommits.single().progressSummaryWrite.subcategoryDeltas shouldBe emptyMap()
    }

    @Test
    fun `a card with no prior entry increments the studied delta, a card with one does not`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-2" to priorEntry(FlashcardStudyProgressState.Partial))),
        )
        val result = ratedSessionResult(
            cardResults = listOf(
                ratedEntry(cardId = "card-1", state = FlashcardStudyProgressState.Failed),
                ratedEntry(cardId = "card-2", state = FlashcardStudyProgressState.Failed),
            ),
        )

        createUseCase().invoke(result)

        singleSummaryDelta() shouldBe SubcategoryProgressSummaryDelta(masteredDelta = 0, studiedDelta = 1)
    }

    @Test
    fun `a Fast card with no prior entry contributes to the studied delta only, never the mastered delta`() = runTest {
        val result = fastSessionResult(cardResults = listOf(fastEntry(cardId = "card-1")))

        createUseCase().invoke(result)

        singleSummaryDelta() shouldBe SubcategoryProgressSummaryDelta(masteredDelta = 0, studiedDelta = 1)
    }

    @Test
    fun `a Fast card revisiting an existing entry contributes nothing to the summary write`() = runTest {
        cardProgressRepository.seed(
            SubcategoryProgress(subcategoryId = "sub-1", categoryId = "cat-1", cards = mapOf("card-1" to priorEntry(FlashcardStudyProgressState.Seen))),
        )
        val result = fastSessionResult(cardResults = listOf(fastEntry(cardId = "card-1")))

        createUseCase().invoke(result)

        studySessionRepository.committedSessionCommits.single().progressSummaryWrite.subcategoryDeltas shouldBe emptyMap()
    }

    @Test
    fun `a session spanning two subcategories produces one summary write carrying both topics' independent deltas`() = runTest {
        val result = ratedSessionResult(
            cardResults = listOf(
                ratedEntry(cardId = "card-1", subcategoryId = "sub-1", state = FlashcardStudyProgressState.Mastered),
                ratedEntry(cardId = "card-2", subcategoryId = "sub-2", state = FlashcardStudyProgressState.Failed),
            ),
        )

        createUseCase().invoke(result)

        val commits = studySessionRepository.committedSessionCommits
        commits.size shouldBe 1
        val deltas = commits.single().progressSummaryWrite.subcategoryDeltas
        deltas.keys shouldBe setOf("sub-1", "sub-2")
        deltas.getValue("sub-1") shouldBe SubcategoryProgressSummaryDelta(masteredDelta = 1, studiedDelta = 1)
        deltas.getValue("sub-2") shouldBe SubcategoryProgressSummaryDelta(masteredDelta = 0, studiedDelta = 1)
    }

    @Test
    fun `the sum of the write's studied deltas equals the session document's newCardsStudied count`() = runTest {
        val result = ratedSessionResult(
            cardResults = listOf(
                ratedEntry(cardId = "card-1", subcategoryId = "sub-1", state = FlashcardStudyProgressState.Mastered),
                ratedEntry(cardId = "card-2", subcategoryId = "sub-2", state = FlashcardStudyProgressState.Failed),
            ),
        )

        createUseCase().invoke(result)

        val commit = studySessionRepository.committedSessionCommits.single()
        val studiedDeltaSum = commit.progressSummaryWrite.subcategoryDeltas.values.sumOf { it.studiedDelta }
        studiedDeltaSum shouldBe commit.newCardsStudied
    }
}
