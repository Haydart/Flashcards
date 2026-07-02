package com.rossomak.flashcards.feature.browse

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.repository.FakeFlashcardRepository
import com.rossomak.flashcards.core.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.core.ui.navigation.RouteDecoder
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubcategoryDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val savedStateHandle: SavedStateHandle = mockk()
    private val flashcardRepository = FakeFlashcardRepository()
    private val getFlashcards = GetFlashcardsUseCase(flashcardRepository)

    private val route = SubcategoryDetailsRoute(
        categoryId = "cat-1",
        categoryName = "Android",
        subcategoryId = "sub-1",
        subcategoryName = "Compose",
    )

    @Before
    fun setUp() {
        mockkObject(RouteDecoder)
        every { RouteDecoder.decode(any<() -> SubcategoryDetailsRoute>()) } returns route
    }

    @After
    fun tearDown() {
        unmockkObject(RouteDecoder)
    }

    private fun createViewModel(): SubcategoryDetailsViewModel =
        SubcategoryDetailsViewModel(savedStateHandle, getFlashcards)

    @Test
    fun `onStartSession emits PreviewStudySession with category and subcategory ids and names`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onStartSession()

        viewModel.events.test {
            awaitItem() shouldBe SubcategoryDetailsDestination.PreviewStudySession(
                categoryId = route.categoryId,
                categoryName = route.categoryName,
                subcategoryId = route.subcategoryId,
                subcategoryName = route.subcategoryName,
            )
        }
    }
}
