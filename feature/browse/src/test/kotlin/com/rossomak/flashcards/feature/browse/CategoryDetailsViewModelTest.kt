package com.rossomak.flashcards.feature.browse

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.model.Subcategory
import com.rossomak.flashcards.core.domain.repository.FakeFlashcardRepository
import com.rossomak.flashcards.core.domain.usecase.GetSubcategoriesUseCase
import com.rossomak.flashcards.core.ui.navigation.RouteDecoder
import com.rossomak.flashcards.testutil.MainDispatcherRule
import com.rossomak.flashcards.testutil.assertValue
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
class CategoryDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val savedStateHandle: SavedStateHandle = mockk()
    private val flashcardRepository = FakeFlashcardRepository()
    private val getSubcategories = GetSubcategoriesUseCase(flashcardRepository)

    private val route = CategoryDetailsRoute(categoryId = "android", categoryName = "Android")

    @Before
    fun setUp() {
        mockkObject(RouteDecoder)
        every { RouteDecoder.decode(any<() -> CategoryDetailsRoute>()) } returns route
    }

    @After
    fun tearDown() {
        unmockkObject(RouteDecoder)
    }

    private fun createViewModel(): CategoryDetailsViewModel =
        CategoryDetailsViewModel(savedStateHandle, getSubcategories)

    private fun subcategory(id: String): Subcategory = Subcategory(
        id = id,
        name = "name-$id",
        categoryId = route.categoryId,
        categoryName = route.categoryName,
        order = 0,
        cardCount = 3,
    )

    @Test
    fun `init seeds state with category id and name from the route`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.state.value.categoryId shouldBe route.categoryId
        viewModel.state.value.categoryName shouldBe route.categoryName
    }

    @Test
    fun `init loads subcategories for the routed category`() = runTest(mainDispatcherRule.testDispatcher) {
        val subcategories = listOf(subcategory("sub-1"), subcategory("sub-2"))
        flashcardRepository.subcategoriesToReturn = Result.success(subcategories)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.assertValue {
            isLoading shouldBe false
            this.subcategories shouldBe subcategories
            error shouldBe null
        }
    }

    @Test
    fun `failed subcategory load surfaces error and stops loading`() = runTest(mainDispatcherRule.testDispatcher) {
        flashcardRepository.subcategoriesToReturn = Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.assertValue {
            isLoading shouldBe false
            subcategories shouldBe emptyList()
            error shouldBe "Could not load topics"
        }
    }

    // --- fake favourite ---

    @Test
    fun `toggling the favourite flips the flag and emits a message without persisting anything`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.messages.test {
                viewModel.onFavoriteToggle()

                awaitItem() shouldBe CategoryDetailsMessage.AddedToFavorites
                viewModel.state.value.isFavorite shouldBe true
            }
        }

    @Test
    fun `undoing the favourite flips it back`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onFavoriteToggle()

        viewModel.onFavoriteUndo(restoreTo = false)

        viewModel.state.value.isFavorite shouldBe false
    }

    @Test
    fun `toggling an already favourited category removes it`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onFavoriteToggle()

            viewModel.messages.test {
                viewModel.onFavoriteToggle()

                awaitItem() shouldBe CategoryDetailsMessage.RemovedFromFavorites
                viewModel.state.value.isFavorite shouldBe false
            }
        }

    /**
     * A snackbar outlives the tap that raised it, so Undo restores the value the toggle moved away
     * from rather than flipping whatever is current.
     */
    @Test
    fun `a stale undo does not invert a later toggle`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onFavoriteToggle()
        viewModel.onFavoriteToggle()

        viewModel.onFavoriteUndo(restoreTo = false)

        viewModel.state.value.isFavorite shouldBe false
    }
}
