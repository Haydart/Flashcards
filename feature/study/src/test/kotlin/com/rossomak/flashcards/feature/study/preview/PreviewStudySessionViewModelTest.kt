package com.rossomak.flashcards.feature.study.preview

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.model.CardSortOrder
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.repository.FakeFlashcardRepository
import com.rossomak.flashcards.core.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.core.ui.navigation.RouteDecoder
import com.rossomak.flashcards.feature.study.PreviewStudySessionRoute
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewStudySessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val savedStateHandle: SavedStateHandle = mockk()
    private val flashcardRepository = FakeFlashcardRepository()
    private val getFlashcards = GetFlashcardsUseCase(flashcardRepository)

    private val categoryId = "android"
    private val categoryName = "Android"
    private val subcategoryId = "android-compose"
    private val subcategoryName = "Compose"

    private val singleTopicRoute = PreviewStudySessionRoute(
        categoryId = categoryId,
        categoryName = categoryName,
        subcategoryIds = listOf(subcategoryId),
        subcategoryNames = listOf(subcategoryName),
    )

    @Before
    fun setUp() {
        mockkObject(RouteDecoder)
    }

    @After
    fun tearDown() {
        unmockkObject(RouteDecoder)
    }

    private fun stubRoute(route: PreviewStudySessionRoute) {
        every { RouteDecoder.decode(any<() -> PreviewStudySessionRoute>()) } returns route
    }

    private fun createViewModel(): PreviewStudySessionViewModel =
        PreviewStudySessionViewModel(savedStateHandle, getFlashcards)

    private fun flashcard(
        id: String,
        subcategoryId: String = this.subcategoryId,
        tags: List<String> = listOf("General"),
        difficulty: Int = 5
    ): Flashcard = Flashcard(
        id = id,
        subcategoryId = subcategoryId,
        tags = tags,
        question = "question-$id",
        answer = "answer-$id",
        difficulty = difficulty,
        questionCode = null,
        answerCode = null,
        questionSpoken = null,
        answerSpoken = null,
        extendedContext = null
    )

    @Test
    fun `selection caps card count at session size`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn =
            Result.success((1..30).map { index -> flashcard(id = "card-$index") })

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.selectedCardCount shouldBe 20
        viewModel.state.value.isLoading shouldBe false
    }

    @Test
    fun `selection uses whole pool when smaller than session size`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn =
            Result.success((1..5).map { index -> flashcard(id = "card-$index") })

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.selectedCardCount shouldBe 5
    }

    @Test
    fun `tag filter keeps only cards carrying any active tag`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute.copy(filterTagIds = listOf("State")))
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "card-1", tags = listOf("State")),
                flashcard(id = "card-2", tags = listOf("Modifiers")),
                flashcard(id = "card-3", tags = listOf("State", "Modifiers")),
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.selectedCardCount shouldBe 2
    }

    @Test
    fun `multi subcategory route pools cards across subcategories`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(
            singleTopicRoute.copy(
                subcategoryIds = listOf("android-compose", "android-coroutines"),
                subcategoryNames = listOf("Compose", "Coroutines"),
            )
        )
        flashcardRepository.flashcardsBySubcategory["android-compose"] =
            Result.success(listOf(flashcard(id = "card-1")))
        flashcardRepository.flashcardsBySubcategory["android-coroutines"] =
            Result.success(listOf(flashcard(id = "card-2", subcategoryId = "android-coroutines")))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.selectedCardCount shouldBe 2
    }

    @Test
    fun `failed fetch surfaces error`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.error shouldBe "Could not load flashcards"
        viewModel.state.value.canStart shouldBe false
    }

    @Test
    fun `onSessionCardCountChange reselects cards at the new count`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn =
            Result.success((1..30).map { index -> flashcard(id = "card-$index") })

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSessionCardCountChange(10)

        viewModel.state.value.sessionCardCount shouldBe 10
        viewModel.state.value.selectedCardCount shouldBe 10
    }

    @Test
    fun `onDifficultyRangeChange filters the pool to the selected band`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "card-1", difficulty = 2),
                flashcard(id = "card-2", difficulty = 5),
                flashcard(id = "card-3", difficulty = 9),
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDifficultyRangeChange(4..6)

        viewModel.state.value.difficultyRange shouldBe 4..6
        viewModel.state.value.selectedCardCount shouldBe 1
    }

    @Test
    fun `onStudyModeSelect updates selected mode`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onStudyModeSelect(StudyMode.Fast)

        viewModel.state.value.selectedStudyMode shouldBe StudyMode.Fast
    }

    @Test
    fun `onSortDialogShow and onSortDialogDismiss toggle dialog visibility`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSortDialogShow()
        viewModel.state.value.isSortDialogVisible shouldBe true

        viewModel.onSortDialogDismiss()
        viewModel.state.value.isSortDialogVisible shouldBe false
    }

    @Test
    fun `onSortOrderSelect orders session cards easiest first`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "card-1", difficulty = 8),
                flashcard(id = "card-2", difficulty = 2),
                flashcard(id = "card-3", difficulty = 5),
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSortOrderSelect(CardSortOrder.EasiestFirst)
        viewModel.onStartSession()

        viewModel.state.value.sortOrder shouldBe CardSortOrder.EasiestFirst
        viewModel.events.test {
            val destination = awaitItem() as PreviewStudySessionDestination.StudySession
            destination.route.cardIds shouldBe listOf("card-2", "card-3", "card-1")
        }
    }

    @Test
    fun `onSortOrderSelect orders session cards hardest first`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "card-1", difficulty = 8),
                flashcard(id = "card-2", difficulty = 2),
                flashcard(id = "card-3", difficulty = 5),
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSortOrderSelect(CardSortOrder.HardestFirst)
        viewModel.onStartSession()

        viewModel.events.test {
            val destination = awaitItem() as PreviewStudySessionDestination.StudySession
            destination.route.cardIds shouldBe listOf("card-1", "card-3", "card-2")
        }
    }

    @Test
    fun `onStartSession emits StudySession route with selected cards and mode`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(flashcard(id = "card-1"), flashcard(id = "card-2"))
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onStudyModeSelect(StudyMode.Fast)
        viewModel.onStartSession()

        viewModel.events.test {
            val destination = awaitItem() as PreviewStudySessionDestination.StudySession
            destination.route.categoryId shouldBe categoryId
            destination.route.sessionTitle shouldBe subcategoryName
            destination.route.subcategoryIds shouldBe listOf(subcategoryId)
            destination.route.cardIds shouldContainAll listOf("card-1", "card-2")
            destination.route.studyMode shouldBe StudyMode.Fast
        }
    }

    @Test
    fun `onStartSession with empty pool emits nothing`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onStartSession()

        viewModel.events.test {
            expectNoEvents()
        }
    }

    @Test
    fun `onStartSession ignores re-entrant calls while a session is already pending`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onStartSession()
        viewModel.onStartSession()

        viewModel.events.test {
            awaitItem() as PreviewStudySessionDestination.StudySession
            expectNoEvents()
        }
    }

    @Test
    fun `onRetry recovers from a previous failure and loads the pool`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.state.value.error shouldBe "Could not load flashcards"

        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))
        viewModel.onRetry()
        advanceUntilIdle()

        viewModel.state.value.error shouldBe null
        viewModel.state.value.selectedCardCount shouldBe 1
    }

    @Test
    fun `sessionTitle uses category name for multi topic sessions`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(
            singleTopicRoute.copy(
                subcategoryIds = listOf("android-compose", "android-coroutines"),
                subcategoryNames = listOf("Compose", "Coroutines"),
            )
        )
        flashcardRepository.flashcardsBySubcategory["android-compose"] =
            Result.success(listOf(flashcard(id = "card-1")))
        flashcardRepository.flashcardsBySubcategory["android-coroutines"] =
            Result.success(listOf(flashcard(id = "card-2", subcategoryId = "android-coroutines")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onStartSession()

        viewModel.events.test {
            val destination = awaitItem() as PreviewStudySessionDestination.StudySession
            destination.route.sessionTitle shouldBe categoryName
        }
    }

    @Test
    fun `estimatedMinutes rounds up to the nearest minute`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn =
            Result.success((1..5).map { index -> flashcard(id = "card-$index") })

        val viewModel = createViewModel()
        advanceUntilIdle()

        // 5 cards * 40s/card = 200s -> ceil(200/60) = 4 minutes
        viewModel.state.value.estimatedMinutes shouldBe 4
    }

    @Test
    fun `quick session on a single subcategory can still rerandomize`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute.copy(isQuickSession = true))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.canRerandomize.shouldBeTrue()
    }

    @Test
    fun `onRerandomize reselects from pool keeping session size`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(
            singleTopicRoute.copy(
                subcategoryIds = listOf("android-compose", "android-coroutines"),
                subcategoryNames = listOf("Compose", "Coroutines"),
            )
        )
        flashcardRepository.flashcardsBySubcategory["android-compose"] =
            Result.success((1..30).map { index -> flashcard(id = "compose-$index") })
        flashcardRepository.flashcardsBySubcategory["android-coroutines"] =
            Result.success((1..30).map { index -> flashcard(id = "coroutines-$index", subcategoryId = "android-coroutines") })

        val viewModel = createViewModel()
        advanceUntilIdle()
        val cardIdsBeforeRerandomize = viewModel.selectedCardIds

        viewModel.onRerandomize()

        viewModel.state.value.selectedCardCount shouldBe 20
        viewModel.selectedCardIds shouldNotBe cardIdsBeforeRerandomize
    }

    @Test
    fun `single topic route cannot rerandomize, multi topic can`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        createViewModel().state.value.canRerandomize shouldBe false

        stubRoute(
            singleTopicRoute.copy(
                subcategoryIds = listOf("android-compose", "android-coroutines"),
                subcategoryNames = listOf("Compose", "Coroutines"),
            )
        )
        createViewModel().state.value.canRerandomize.shouldBeTrue()
    }
}
