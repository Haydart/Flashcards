package com.rossomak.flashcards.presentation.home

import com.rossomak.flashcards.testutil.MainDispatcherRule
import com.rossomak.flashcards.testutil.assertValue
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        viewModel = HomeViewModel()
    }

    @Test
    fun `initial state has no navigation destination`() {
        viewModel.state.assertValue {
            navigationDestination shouldBe null
        }
    }

    @Test
    fun `onNavigationHandled clears CategoryDetails destination`() {
        viewModel.navigateTo(HomeDestination.CategoryDetails("cat1"))

        viewModel.onNavigationHandled()

        viewModel.state.assertValue {
            navigationDestination shouldBe null
        }
    }

    @Test
    fun `onNavigationHandled clears SubcategoryDetails destination`() {
        viewModel.navigateTo(HomeDestination.SubcategoryDetails("cat1", "sub1"))

        viewModel.onNavigationHandled()

        viewModel.state.assertValue {
            navigationDestination shouldBe null
        }
    }

    @Test
    fun `onNavigationHandled is idempotent when destination already null`() {
        viewModel.onNavigationHandled()

        viewModel.state.assertValue {
            navigationDestination shouldBe null
        }
    }
}
