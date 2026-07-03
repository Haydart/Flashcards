package com.rossomak.flashcards.feature.home

import io.kotest.matchers.shouldBe
import org.junit.Test

class HomeViewModelTest {

    private fun createViewModel(): HomeViewModel = HomeViewModel()

    @Test
    fun `initial state exposes the default home screen state`() {
        val viewModel = createViewModel()

        viewModel.state.value shouldBe HomeScreenState()
    }
}
