package com.rossomak.flashcards.feature.browse

import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.model.Category
import com.rossomak.flashcards.core.domain.model.Subcategory
import com.rossomak.flashcards.core.domain.repository.FakeFlashcardRepository
import com.rossomak.flashcards.core.domain.usecase.GetCategoriesUseCase
import com.rossomak.flashcards.core.domain.usecase.SearchCategoriesUseCase
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val flashcardRepository = FakeFlashcardRepository()
    private val getCategories = GetCategoriesUseCase(flashcardRepository)
    private val searchCategories = SearchCategoriesUseCase(flashcardRepository)

    private val categoryId = "cat-1"
    private val categoryName = "Android"

    private val android = Category(
        id = categoryId,
        name = categoryName,
        order = 0,
        subcategoryCount = 2,
        iconSvg = null,
        color = null,
        featuredSubcategoryNames = listOf("Compose", "Coroutines"),
    )

    private val compose = Subcategory(
        id = "android-compose",
        name = "Compose",
        categoryId = categoryId,
        categoryName = categoryName,
        order = 0,
        cardCount = 12,
    )

    private fun createViewModel(): BrowseViewModel =
        BrowseViewModel(getCategories, searchCategories)

    @Test
    fun `onCategorySelected emits CategoryDetails with id and name`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onCategorySelected(categoryId, categoryName)

        viewModel.events.test {
            awaitItem() shouldBe BrowseNavigationDestination.CategoryDetails(categoryId, categoryName)
        }
    }

    @Test
    fun `onSubcategorySelect emits SubcategoryDetails carrying the parent from the matched subcategory`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onSubcategorySelect(compose)

            viewModel.events.test {
                awaitItem() shouldBe BrowseNavigationDestination.SubcategoryDetails(
                    categoryId = categoryId,
                    categoryName = categoryName,
                    subcategoryId = compose.id,
                    subcategoryName = compose.name,
                )
            }
        }

    @Test
    fun `onSubcategorySessionStart emits PreviewStudySession`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onSubcategorySessionStart(compose)

        viewModel.events.test {
            awaitItem() shouldBe BrowseNavigationDestination.PreviewStudySession(
                categoryId = categoryId,
                categoryName = categoryName,
                subcategoryId = compose.id,
                subcategoryName = compose.name,
            )
        }
    }

    @Test
    fun `a typed query is not searched until the debounce elapses`() = runTest(mainDispatcherRule.testDispatcher) {
        flashcardRepository.categoriesToReturn = Result.success(listOf(android))
        flashcardRepository.searchResultsByPrefix["compose"] = Result.success(listOf(compose))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchQueryChange("compose")

        advanceTimeBy(DEBOUNCE_MILLIS - 1)
        flashcardRepository.searchedPrefixes shouldContainExactly emptyList()

        advanceUntilIdle()
        flashcardRepository.searchedPrefixes shouldContainExactly listOf("compose")
        viewModel.state.value.searchResults?.subcategories shouldContainExactly listOf(compose)
    }

    @Test
    fun `keystrokes inside the debounce window only search the final query`() =
        runTest(mainDispatcherRule.testDispatcher) {
            flashcardRepository.categoriesToReturn = Result.success(listOf(android))
            flashcardRepository.searchResultsByPrefix["compose"] = Result.success(listOf(compose))

            val viewModel = createViewModel()
            advanceUntilIdle()

            listOf("co", "com", "compose").forEach { query ->
                viewModel.onSearchQueryChange(query)
                advanceTimeBy(DEBOUNCE_MILLIS / 2)
            }
            advanceUntilIdle()

            flashcardRepository.searchedPrefixes shouldContainExactly listOf("compose")
        }

    @Test
    fun `a query below the minimum length falls back to the default list rather than no matches`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("c")
            advanceUntilIdle()

            viewModel.state.value.searchResults shouldBe null
            flashcardRepository.searchedPrefixes shouldContainExactly emptyList()
        }

    @Test
    fun `a failed search reports an error instead of claiming nothing matched`() =
        runTest(mainDispatcherRule.testDispatcher) {
            flashcardRepository.searchResultsByPrefix["compose"] = Result.failure(IllegalStateException("offline"))

            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSearchQueryChange("compose")
            advanceUntilIdle()

            viewModel.state.value.hasSearchError shouldBe true
            viewModel.state.value.searchResults shouldBe null
        }

    @Test
    fun `onSearchDismiss empties the query and leaves search`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onSearchActivate()
        viewModel.onSearchQueryChange("compose")

        viewModel.onSearchDismiss()

        viewModel.state.value.searchQuery shouldBe ""
        viewModel.state.value.isSearchActive shouldBe false
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 500L
    }
}
