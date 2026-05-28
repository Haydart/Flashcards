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

        viewModel.navigationDestination.value shouldBe null
    }

    @Test
    fun `animation completed before timeout with user navigates to Main`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser

        val viewModel = createViewModel()
        viewModel.onAnimationCompleted()
        advanceUntilIdle()

        viewModel.navigationDestination.value shouldBe SplashDestination.Main
    }

    @Test
    fun `animation completed before timeout with null user navigates to Login`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns null

        val viewModel = createViewModel()
        viewModel.onAnimationCompleted()
        advanceUntilIdle()

        viewModel.navigationDestination.value shouldBe SplashDestination.Login
    }

    @Test
    fun `auth timeout alone without animation completing keeps destination null`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns null

        val viewModel = createViewModel()
        // Do NOT call onAnimationCompleted - simulate animation hang.
        advanceUntilIdle()

        // Auth resolves to false after 1000ms timeout, but animation hasn't completed,
        // so combine never emits a destination.
        viewModel.navigationDestination.value shouldBe null
        testScheduler.currentTime shouldBeLessThan 7_000L
    }

    @Test
    fun `navigation destination resolves without extra delay when animation and auth both complete`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser

        val viewModel = createViewModel()
        viewModel.onAnimationCompleted()
        advanceUntilIdle()

        viewModel.navigationDestination.value shouldBe SplashDestination.Main
        // No post-animation delay in current implementation - destination set immediately.
        testScheduler.currentTime shouldBeLessThan 2_000L
    }

    @Test
    fun `onAnimationCompleted called multiple times still navigates to expected destination`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser

        val viewModel = createViewModel()
        viewModel.onAnimationCompleted()
        viewModel.onAnimationCompleted()
        advanceUntilIdle()

        viewModel.navigationDestination.value shouldBe SplashDestination.Main
    }

    @Test
    fun `onAnimationCompleted called after timeout does not change navigation destination`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAnimationCompleted()
        advanceUntilIdle()

        viewModel.navigationDestination.value shouldBe SplashDestination.Login
    }
}
