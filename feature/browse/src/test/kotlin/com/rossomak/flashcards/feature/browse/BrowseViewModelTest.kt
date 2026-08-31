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
import io.kotest.matchers.types.shouldBeInstanceOf
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
        val status = viewModel.state.value.searchStatus
        status.shouldBeInstanceOf<SearchStatus.Results>()
        status.results.subcategories shouldContainExactly listOf(compose)
    }

    @Test
    fun `a query still below the debounce reports loading rather than the too-short prompt once it is long enough`() =
        runTest(mainDispatcherRule.testDispatcher) {
            flashcardRepository.categoriesToReturn = Result.success(listOf(android))
            flashcardRepository.searchResultsByPrefix["ap"] = Result.success(emptyList())

            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSearchQueryChange("app")
            advanceUntilIdle()
            viewModel.onSearchQueryChange("ap")

            viewModel.state.value.searchStatus shouldBe SearchStatus.Loading

            advanceUntilIdle()
            flashcardRepository.searchedPrefixes shouldContainExactly listOf("app", "ap")
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
    fun `a query below the minimum length reports the prompt status rather than no matches`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("c")

            viewModel.state.value.searchStatus shouldBe SearchStatus.Prompt

            advanceUntilIdle()
            flashcardRepository.searchedPrefixes shouldContainExactly emptyList()
        }

    @Test
    fun `a query at or above the minimum length reports loading until the debounce elapses`() =
        runTest(mainDispatcherRule.testDispatcher) {
            flashcardRepository.categoriesToReturn = Result.success(listOf(android))
            flashcardRepository.searchResultsByPrefix["compose"] = Result.success(listOf(compose))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("compose")

            viewModel.state.value.searchStatus shouldBe SearchStatus.Loading
        }

    @Test
    fun `a search matching nothing reports NoMatch rather than Results`() =
        runTest(mainDispatcherRule.testDispatcher) {
            flashcardRepository.searchResultsByPrefix["xyz"] = Result.success(emptyList())

            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSearchQueryChange("xyz")
            advanceUntilIdle()

            viewModel.state.value.searchStatus shouldBe SearchStatus.NoMatch
        }

    @Test
    fun `a failed search reports Error instead of claiming nothing matched`() =
        runTest(mainDispatcherRule.testDispatcher) {
            flashcardRepository.searchResultsByPrefix["compose"] = Result.failure(IllegalStateException("offline"))

            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSearchQueryChange("compose")
            advanceUntilIdle()

            viewModel.state.value.searchStatus shouldBe SearchStatus.Error
        }

    @Test
    fun `onSearchDismiss empties the query and leaves search`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onSearchActivate()
        viewModel.onSearchQueryChange("compose")

        viewModel.onSearchDismiss()

        viewModel.state.value.searchQuery shouldBe ""
        viewModel.state.value.isSearchActive shouldBe false
        viewModel.state.value.searchStatus shouldBe SearchStatus.Prompt
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 500L
    }
}
