package com.rossomak.flashcards.presentation.splash

import com.rossomak.flashcards.domain.model.AuthUser
import com.rossomak.flashcards.domain.usecase.GetCurrentAuthUserUseCase
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentAuthUserUseCase: GetCurrentAuthUserUseCase = mockk()

    private val testUser = AuthUser("u1", "a@b.com", "Alex", null)

    private fun createViewModel(): SplashViewModel =
        SplashViewModel(getCurrentAuthUserUseCase)

    @Test
    fun `navigationDestination is null before coroutine runs`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser

        val viewModel = createViewModel()

        viewModel.state.value.navigationDestination shouldBe null
    }

    @Test
    fun `animation completed before timeout with user navigates to Main`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser

        val viewModel = createViewModel()
        viewModel.onAnimationCompleted()
        advanceUntilIdle()

        viewModel.state.value.navigationDestination shouldBe SplashDestination.Main
    }

    @Test
    fun `animation completed before timeout with null user navigates to Login`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns null

        val viewModel = createViewModel()
        viewModel.onAnimationCompleted()
        advanceUntilIdle()

        viewModel.state.value.navigationDestination shouldBe SplashDestination.Login
    }

    @Test
    fun `animation timeout navigates without applying post-animation delay`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns null

        val viewModel = createViewModel()
        // Do NOT call onAnimationCompleted - simulate animation hang.
        advanceUntilIdle()

        viewModel.state.value.navigationDestination shouldBe SplashDestination.Login
        // Total virtual time must be ~5000ms (timeout only), NOT 7000ms (timeout + 2000 post-delay).
        // Regression guard for commit 1e181aa.
        testScheduler.currentTime shouldBeLessThan 7_000L
    }

    @Test
    fun `animation completed triggers post-animation delay before navigating`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser

        val viewModel = createViewModel()
        viewModel.onAnimationCompleted()
        advanceUntilIdle()

        viewModel.state.value.navigationDestination shouldBe SplashDestination.Main
        // Animation completed branch must include the 2s post-animation delay.
        (testScheduler.currentTime >= 2_000L) shouldBe true
    }
}
