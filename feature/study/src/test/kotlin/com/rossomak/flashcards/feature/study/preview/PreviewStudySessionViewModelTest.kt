package com.rossomak.flashcards.feature.study.preview

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.repository.FakeFlashcardRepository
import com.rossomak.flashcards.core.domain.usecase.SelectSessionFlashcardsUseCase
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

    private val multiTopicRoute = singleTopicRoute.copy(
        subcategoryIds = listOf("android-compose", "android-coroutines"),
        subcategoryNames = listOf("Compose", "Coroutines"),
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
        PreviewStudySessionViewModel(savedStateHandle, SelectSessionFlashcardsUseCase(flashcardRepository))

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
    fun `routed tag filter seeds the config and keeps only cards carrying an active tag`() =
        runTest(mainDispatcherRule.testDispatcher) {
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

            viewModel.state.value.config.tagIds shouldBe setOf("State")
            viewModel.state.value.selectedCardCount shouldBe 2
        }

    @Test
    fun `multi subcategory route pools cards across subcategories`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(multiTopicRoute)
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
    fun `available tags come from the pool for single topic sessions only`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "card-1", tags = listOf("State")),
                flashcard(id = "card-2", tags = listOf("Modifiers", "State")),
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.availableTags shouldBe listOf("Modifiers", "State")

        stubRoute(multiTopicRoute)
        flashcardRepository.flashcardsBySubcategory["android-compose"] =
            Result.success(listOf(flashcard(id = "card-1", tags = listOf("State"))))
        flashcardRepository.flashcardsBySubcategory["android-coroutines"] =
            Result.success(listOf(flashcard(id = "card-2", subcategoryId = "android-coroutines")))

        val multiTopicViewModel = createViewModel()
        advanceUntilIdle()

        multiTopicViewModel.state.value.availableTags shouldBe emptyList()
    }

    @Test
    fun `opening a dialog seeds its draft from the committed config`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute.copy(filterTagIds = listOf("State")))
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDialogEvent(PreviewDialogEvent.Open.Mode)
        viewModel.state.value.activeDialog shouldBe PreviewDialog.Mode(draft = StudyMode.Rated)

        viewModel.onDialogEvent(PreviewDialogEvent.Open.Length)
        viewModel.state.value.activeDialog shouldBe PreviewDialog.Length(draft = 20)

        viewModel.onDialogEvent(PreviewDialogEvent.Open.Sort)
        viewModel.state.value.activeDialog shouldBe PreviewDialog.Sort(draft = FlashcardSortOrder.Default)

        viewModel.onDialogEvent(PreviewDialogEvent.Open.Filters)
        val filtersDialog = viewModel.state.value.activeDialog as PreviewDialog.Filters
        filtersDialog.draft.selectedTags shouldBe setOf("State")
        filtersDialog.draft.difficultyRange shouldBe 1..10
    }

    @Test
    fun `a draft change leaves the committed config untouched until confirm`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(PreviewDialogEvent.Open.Mode)
        viewModel.onDialogEvent(PreviewDialogEvent.DraftChange.Mode(StudyMode.Fast))

        viewModel.state.value.activeDialog shouldBe PreviewDialog.Mode(draft = StudyMode.Fast)
        viewModel.state.value.config.mode shouldBe StudyMode.Rated
    }

    @Test
    fun `dismissing discards the draft`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(PreviewDialogEvent.Open.Sort)
        viewModel.onDialogEvent(PreviewDialogEvent.DraftChange.SortOrder(FlashcardSortOrder.HardestFirst))
        viewModel.onDialogEvent(PreviewDialogEvent.Dismiss)

        viewModel.state.value.activeDialog shouldBe null
        viewModel.state.value.config.sortOrder shouldBe FlashcardSortOrder.Default
    }

    @Test
    fun `reopening a dialog seeds a fresh draft and clears keep as default`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(PreviewDialogEvent.Open.Sort)
        viewModel.onDialogEvent(PreviewDialogEvent.DraftChange.SortOrder(FlashcardSortOrder.HardestFirst))
        viewModel.onDialogEvent(PreviewDialogEvent.DraftChange.KeepAsDefault(true))
        viewModel.onDialogEvent(PreviewDialogEvent.Confirm)
        advanceUntilIdle()

        viewModel.onDialogEvent(PreviewDialogEvent.Open.Sort)

        viewModel.state.value.activeDialog shouldBe PreviewDialog.Sort(
            draft = FlashcardSortOrder.HardestFirst,
            keepAsDefault = false,
        )
    }

    @Test
    fun `keep as default is ignored while the filters dialog is open`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(PreviewDialogEvent.Open.Filters)
        val dialogBefore = viewModel.state.value.activeDialog

        viewModel.onDialogEvent(PreviewDialogEvent.DraftChange.KeepAsDefault(true))

        viewModel.state.value.activeDialog shouldBe dialogBefore
    }

    @Test
    fun `confirming the mode dialog commits the draft`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(PreviewDialogEvent.Open.Mode)
        viewModel.onDialogEvent(PreviewDialogEvent.DraftChange.Mode(StudyMode.Fast))
        viewModel.onDialogEvent(PreviewDialogEvent.Confirm)
        advanceUntilIdle()

        viewModel.state.value.config.mode shouldBe StudyMode.Fast
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `confirming the length dialog reselects at the new count`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn =
            Result.success((1..30).map { index -> flashcard(id = "card-$index") })

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(PreviewDialogEvent.Open.Length)
        viewModel.onDialogEvent(PreviewDialogEvent.DraftChange.Length(10))
        viewModel.onDialogEvent(PreviewDialogEvent.Confirm)
        advanceUntilIdle()

        viewModel.state.value.config.length shouldBe 10
        viewModel.state.value.selectedCardCount shouldBe 10
    }

    @Test
    fun `confirming the filters dialog narrows the pool by difficulty and tags`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "card-1", tags = listOf("State"), difficulty = 2),
                flashcard(id = "card-2", tags = listOf("State"), difficulty = 5),
                flashcard(id = "card-3", tags = listOf("Modifiers"), difficulty = 5),
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(PreviewDialogEvent.Open.Filters)
        viewModel.onDialogEvent(PreviewDialogEvent.DraftChange.FilterTag("State", isSelected = true))
        viewModel.onDialogEvent(PreviewDialogEvent.DraftChange.FilterDifficulty(4..6))
        viewModel.onDialogEvent(PreviewDialogEvent.Confirm)
        advanceUntilIdle()

        viewModel.state.value.config.tagIds shouldBe setOf("State")
        viewModel.state.value.config.difficultyRange shouldBe 4..6
        viewModel.state.value.selectedCardCount shouldBe 1
    }

    @Test
    fun `unselecting a tag in the filters draft removes it`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute.copy(filterTagIds = listOf("State")))
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1", tags = listOf("State"))))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(PreviewDialogEvent.Open.Filters)
        viewModel.onDialogEvent(PreviewDialogEvent.DraftChange.FilterTag("State", isSelected = false))
        viewModel.onDialogEvent(PreviewDialogEvent.Confirm)
        advanceUntilIdle()

        viewModel.state.value.config.tagIds shouldBe emptySet()
    }

    @Test
    fun `confirming the sort dialog orders session cards easiest first`() = runTest(mainDispatcherRule.testDispatcher) {
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
        viewModel.onDialogEvent(PreviewDialogEvent.Open.Sort)
        viewModel.onDialogEvent(PreviewDialogEvent.DraftChange.SortOrder(FlashcardSortOrder.EasiestFirst))
        viewModel.onDialogEvent(PreviewDialogEvent.Confirm)
        advanceUntilIdle()
        viewModel.onStartSession()

        viewModel.state.value.config.sortOrder shouldBe FlashcardSortOrder.EasiestFirst
        viewModel.events.test {
            val destination = awaitItem() as PreviewStudySessionDestination.StudySession
            destination.route.cardIds shouldBe listOf("card-2", "card-3", "card-1")
        }
    }

    @Test
    fun `confirming the sort dialog orders session cards hardest first`() = runTest(mainDispatcherRule.testDispatcher) {
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
        viewModel.onDialogEvent(PreviewDialogEvent.Open.Sort)
        viewModel.onDialogEvent(PreviewDialogEvent.DraftChange.SortOrder(FlashcardSortOrder.HardestFirst))
        viewModel.onDialogEvent(PreviewDialogEvent.Confirm)
        advanceUntilIdle()
        viewModel.onStartSession()

        viewModel.events.test {
            val destination = awaitItem() as PreviewStudySessionDestination.StudySession
            destination.route.cardIds shouldBe listOf("card-1", "card-3", "card-2")
        }
    }

    @Test
    fun `onStartSession emits StudySession route with selected cards, mode and voice answering`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubRoute(singleTopicRoute)
            flashcardRepository.flashcardsToReturn = Result.success(
                listOf(flashcard(id = "card-1"), flashcard(id = "card-2"))
            )

            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onDialogEvent(PreviewDialogEvent.Open.VoiceAnswering)
            viewModel.onDialogEvent(PreviewDialogEvent.DraftChange.VoiceAnswering(true))
            viewModel.onDialogEvent(PreviewDialogEvent.Confirm)
            advanceUntilIdle()
            viewModel.onStartSession()

            viewModel.events.test {
                val destination = awaitItem() as PreviewStudySessionDestination.StudySession
                destination.route.categoryId shouldBe categoryId
                destination.route.sessionTitle shouldBe subcategoryName
                destination.route.subcategoryIds shouldBe listOf(subcategoryId)
                destination.route.cardIds shouldContainAll listOf("card-1", "card-2")
                destination.route.studyMode shouldBe StudyMode.Rated
                destination.route.voiceAnsweringEnabled shouldBe true
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
        stubRoute(multiTopicRoute)
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
    fun `onRerandomize redraws with a new seed, keeping session size`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(multiTopicRoute)
        flashcardRepository.flashcardsBySubcategory["android-compose"] =
            Result.success((1..30).map { index -> flashcard(id = "compose-$index") })
        flashcardRepository.flashcardsBySubcategory["android-coroutines"] =
            Result.success((1..30).map { index -> flashcard(id = "coroutines-$index", subcategoryId = "android-coroutines") })

        val viewModel = createViewModel()
        advanceUntilIdle()
        val cardIdsBeforeRerandomize = viewModel.selectedCardIds
        val seedBeforeRerandomize = viewModel.state.value.config.seed

        viewModel.onRerandomize()
        advanceUntilIdle()

        viewModel.state.value.config.seed shouldNotBe seedBeforeRerandomize
        viewModel.state.value.selectedCardCount shouldBe 20
        viewModel.selectedCardIds shouldNotBe cardIdsBeforeRerandomize
    }

    @Test
    fun `single topic route cannot rerandomize, multi topic can`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        createViewModel().state.value.canRerandomize shouldBe false

        stubRoute(multiTopicRoute)
        createViewModel().state.value.canRerandomize.shouldBeTrue()
    }
}
