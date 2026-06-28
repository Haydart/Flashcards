package com.rossomak.flashcards.presentation.settings

import com.rossomak.flashcards.core.domain.usecase.SignOutUseCase
import com.rossomak.flashcards.testutil.MainDispatcherRule
import com.rossomak.flashcards.testutil.assertValue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val signOutUseCase: SignOutUseCase = mockk()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        viewModel = SettingsViewModel(signOutUseCase)
    }

    @Test
    fun `initial state has isSigningOut false and no navigation destination`() {
        viewModel.state.assertValue {
            isSigningOut shouldBe false
            navigationDestination shouldBe null
        }
    }

    @Test
    fun `onSignOutClick sets isSigningOut true immediately`() {
        coEvery { signOutUseCase() } returns Unit

        viewModel.onSignOutClick()

        viewModel.state.assertValue {
            isSigningOut shouldBe true
        }
    }

    @Test
    fun `onSignOutClick on success navigates to Login and clears isSigningOut`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { signOutUseCase() } returns Unit

        viewModel.onSignOutClick()
        advanceUntilIdle()

        viewModel.state.assertValue {
            isSigningOut shouldBe false
            navigationDestination shouldBe SettingsDestination.Login
        }
        coVerify(exactly = 1) { signOutUseCase() }
    }

    @Test
    fun `onSignOutClick navigates to Login even when useCase throws`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { signOutUseCase() } throws RuntimeException("network error")

        viewModel.onSignOutClick()
        advanceUntilIdle()

        viewModel.state.assertValue {
            isSigningOut shouldBe false
            navigationDestination shouldBe SettingsDestination.Login
        }
    }

    @Test
    fun `onSignOutClick is no-op when already signing out`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { signOutUseCase() } returns Unit

        viewModel.onSignOutClick()
        viewModel.onSignOutClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { signOutUseCase() }
    }
}
