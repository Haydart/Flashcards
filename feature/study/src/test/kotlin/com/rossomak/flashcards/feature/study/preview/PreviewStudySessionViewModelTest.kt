package com.rossomak.flashcards.feature.study.preview

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
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

    private fun flashcard(id: String, subcategoryId: String = this.subcategoryId, tags: List<String> = listOf("General")): Flashcard =
        Flashcard(
            id = id,
            subcategoryId = subcategoryId,
            tags = tags,
            question = "question-$id",
            answer = "answer-$id",
            difficulty = 5,
            questionCode = null,
            answerCode = null,
            questionSpoken = null,
            answerSpoken = null,
            extendedContext = null,
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
    fun `onStudyModeSelect updates selected mode`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onStudyModeSelect(StudyMode.FAST)

        viewModel.state.value.selectedStudyMode shouldBe StudyMode.FAST
    }

    @Test
    fun `onStartSession emits StudySession route with selected cards and mode`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(flashcard(id = "card-1"), flashcard(id = "card-2"))
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onStudyModeSelect(StudyMode.FAST)
        viewModel.onStartSession()

        viewModel.events.test {
            val destination = awaitItem() as PreviewStudySessionDestination.StudySession
            destination.route.categoryId shouldBe categoryId
            destination.route.sessionTitle shouldBe subcategoryName
            destination.route.subcategoryIds shouldBe listOf(subcategoryId)
            destination.route.cardIds shouldContainAll listOf("card-1", "card-2")
            destination.route.studyMode shouldBe StudyMode.FAST
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
    fun `onRerandomize reselects from pool keeping session size`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn =
            Result.success((1..30).map { index -> flashcard(id = "card-$index") })

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onRerandomize()

        viewModel.state.value.selectedCardCount shouldBe 20
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
