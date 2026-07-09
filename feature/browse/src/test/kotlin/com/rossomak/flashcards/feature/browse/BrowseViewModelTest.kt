package com.rossomak.flashcards.feature.browse

import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.repository.FakeFlashcardRepository
import com.rossomak.flashcards.core.domain.usecase.GetCategoriesUseCase
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val flashcardRepository = FakeFlashcardRepository()
    private val getCategories = GetCategoriesUseCase(flashcardRepository)

    private val categoryId = "cat-1"
    private val categoryName = "Android"

    private fun createViewModel(): BrowseViewModel = BrowseViewModel(getCategories)

    @Test
    fun `onCategorySelected emits CategoryDetails with id and name`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onCategorySelected(categoryId, categoryName)

        viewModel.events.test {
            awaitItem() shouldBe BrowseNavigationDestination.CategoryDetails(categoryId, categoryName)
        }
    }
}
