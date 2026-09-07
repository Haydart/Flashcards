package com.rossomak.flashcards.feature.study.summary

import androidx.lifecycle.SavedStateHandle
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.repository.FakeCardProgressRepository
import com.rossomak.flashcards.core.domain.repository.FakeStudySessionRepository
import com.rossomak.flashcards.core.domain.usecase.CommitStudySessionUseCase
import com.rossomak.flashcards.core.ui.navigation.RouteDecoder
import com.rossomak.flashcards.feature.study.StudySessionSummaryRoute
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * There is no past-session fallback path to test here — this route only ever carries a fresh
 * result (spec 03 ticket 02), so every case below is the same single load path.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StudySessionSummaryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val savedStateHandle: SavedStateHandle = mockk()
    private val studySessionRepository = FakeStudySessionRepository()
    private val cardProgressRepository = FakeCardProgressRepository()

    private fun createViewModel(): StudySessionSummaryViewModel = StudySessionSummaryViewModel(
        savedStateHandle,
        CommitStudySessionUseCase(studySessionRepository, cardProgressRepository),
    )

    @Before
    fun setUp() {
        mockkObject(RouteDecoder)
    }

    @After
    fun tearDown() {
        unmockkObject(RouteDecoder)
    }

    private fun stubRoute(route: StudySessionSummaryRoute) {
        every { RouteDecoder.decode(any<() -> StudySessionSummaryRoute>()) } returns route
    }

    private fun ratedRoute(
        sessionId: String = "session-1",
        abandoned: Boolean = false,
        cardStates: List<FlashcardStudyProgressState> = listOf(
            FlashcardStudyProgressState.Mastered,
            FlashcardStudyProgressState.Mastered,
            FlashcardStudyProgressState.Partial,
            FlashcardStudyProgressState.Failed,
        ),
    ): StudySessionSummaryRoute {
        val cardIds = cardStates.indices.map { "card-$it" }
        return StudySessionSummaryRoute(
            sessionId = sessionId,
            mode = StudyMode.Rated,
            startedAtEpochSecond = 0L,
            durationSeconds = 120,
            abandoned = abandoned,
            categoryId = "cat-1",
            categoryName = "Category",
            subcategoryIds = listOf("sub-1"),
            subcategoryNames = listOf("Subcategory"),
            cardIds = cardIds,
            cardSubcategoryIds = cardIds.map { "sub-1" },
            cardStates = cardStates,
            cardAttemptsUsed = cardStates.map { 1 },
            cardWasPreviouslyMastered = cardStates.map { false },
        )
    }

    @Test
    fun `a Rated result exposes mode, duration, studied count and the three Terminal State counts`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubRoute(ratedRoute())

            val viewModel = createViewModel()
            advanceUntilIdle()

            with(viewModel.state.value) {
                mode shouldBe StudyMode.Rated
                durationSeconds shouldBe 120
                studiedCount shouldBe 4
                abandoned shouldBe false
                masteredCount shouldBe 2
                partialCount shouldBe 1
                failedCount shouldBe 1
            }
        }

    @Test
    fun `an abandoned Rated result exposes the abandoned flag set`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(ratedRoute(abandoned = true))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.abandoned shouldBe true
    }

    @Test
    fun `a Fast result exposes the reduced set with zero for every Terminal State count`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val cardIds = listOf("card-1", "card-2")
            stubRoute(
                StudySessionSummaryRoute(
                    sessionId = "session-2",
                    mode = StudyMode.Fast,
                    startedAtEpochSecond = 0L,
                    durationSeconds = 60,
                    abandoned = false,
                    categoryId = "cat-1",
                    categoryName = "Category",
                    subcategoryIds = listOf("sub-1"),
                    subcategoryNames = listOf("Subcategory"),
                    cardIds = cardIds,
                    cardSubcategoryIds = cardIds.map { "sub-1" },
                    cardStates = cardIds.map { FlashcardStudyProgressState.Seen },
                    // Rated-only (ADR-0014): null for a Fast route, not zero-filled lists.
                    cardAttemptsUsed = null,
                    cardWasPreviouslyMastered = null,
                ),
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            with(viewModel.state.value) {
                mode shouldBe StudyMode.Fast
                studiedCount shouldBe 2
                masteredCount shouldBe 0
                partialCount shouldBe 0
                failedCount shouldBe 0
            }
        }

    @Test
    fun `arriving at the summary commits the session exactly once`() = runTest(mainDispatcherRule.testDispatcher) {
        val route = ratedRoute()
        stubRoute(route)

        val viewModel = createViewModel()
        advanceUntilIdle()

        studySessionRepository.committedSessionCommits.size shouldBe 1
        studySessionRepository.committedSessionCommits.single().sessionResult.id shouldBe route.sessionId
        // Configuration change re-observes the same ViewModel instance rather than recreating it,
        // so a second read of state must not trigger a second commit.
        viewModel.state.value
        studySessionRepository.committedSessionCommits.size shouldBe 1
    }

    @Test
    fun `a queued offline commit emits no message and leaves the displayed results intact`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubRoute(ratedRoute())
            studySessionRepository.commitResultToReturn = Result.success(Unit)
            var messageReceived = false

            val viewModel = createViewModel()
            val collectJob = launch { viewModel.messages.collect { messageReceived = true } }
            advanceUntilIdle()

            messageReceived shouldBe false
            viewModel.state.value.studiedCount shouldBe 4
            collectJob.cancel()
        }

    @Test
    fun `a synchronously rejected commit surfaces a non-blocking message without clearing results`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubRoute(ratedRoute())
            studySessionRepository.commitResultToReturn = Result.failure(IllegalStateException("no authenticated user"))
            var receivedMessage: StudySessionSummaryMessage? = null

            val viewModel = createViewModel()
            val collectJob = launch { viewModel.messages.collect { message -> receivedMessage = message } }
            advanceUntilIdle()

            receivedMessage shouldBe StudySessionSummaryMessage.SaveFailed
            viewModel.state.value.studiedCount shouldBe 4
            collectJob.cancel()
        }

    @Test
    fun `a later async rejection surfaces the same non-blocking message`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubRoute(ratedRoute())
            studySessionRepository.commitResultToReturn = Result.success(Unit)
            studySessionRepository.rejectionToDeliver = IllegalStateException("permission denied")
            var receivedMessage: StudySessionSummaryMessage? = null

            val viewModel = createViewModel()
            val collectJob = launch { viewModel.messages.collect { message -> receivedMessage = message } }
            advanceUntilIdle()

            receivedMessage shouldBe StudySessionSummaryMessage.SaveFailed
            viewModel.state.value.studiedCount shouldBe 4
            collectJob.cancel()
        }
}
