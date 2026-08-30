package com.rossomak.flashcards.presentation.startup

import com.rossomak.flashcards.core.domain.model.AuthUser
import com.rossomak.flashcards.core.domain.usecase.GetCurrentAuthUserUseCase
import com.rossomak.flashcards.core.domain.usecase.SyncFlashcardCacheGenerationUseCase
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppStartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentAuthUserUseCase: GetCurrentAuthUserUseCase = mockk()
    private val syncFlashcardCacheGenerationUseCase: SyncFlashcardCacheGenerationUseCase = mockk {
        coEvery { this@mockk() } returns Unit
    }

    private val testUser = AuthUser("u1", "a@b.com", "Alex", null)

    private fun createViewModel(): AppStartViewModel =
        AppStartViewModel(getCurrentAuthUserUseCase, syncFlashcardCacheGenerationUseCase)

    @Test
    fun `startupState initial value is Loading`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser

        val viewModel = createViewModel()

        viewModel.startupState.value shouldBe AppStartupState.Loading
    }

    @Test
    fun `authenticated user emits Ready with true`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startupState.value shouldBe AppStartupState.Ready(authenticated = true)
    }

    @Test
    fun `no user emits Ready with false`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startupState.value shouldBe AppStartupState.Ready(authenticated = false)
    }

    @Test
    fun `auth timeout emits Ready with false after 800ms`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } coAnswers {
            delay(Long.MAX_VALUE)
            null
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startupState.value shouldBe AppStartupState.Ready(authenticated = false)
        testScheduler.currentTime shouldBe 800L
    }

    @Test
    fun `startup runs the cache generation sync alongside the auth check`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser

        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { syncFlashcardCacheGenerationUseCase() }
    }

    @Test
    fun `a hanging cache generation sync does not delay Ready`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser
        coEvery { syncFlashcardCacheGenerationUseCase() } coAnswers { delay(Long.MAX_VALUE) }

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startupState.value shouldBe AppStartupState.Ready(authenticated = true)
    }
}
