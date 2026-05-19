package com.rossomak.flashcards.presentation.main

import com.rossomak.flashcards.domain.model.AuthUser
import com.rossomak.flashcards.domain.usecase.GetCurrentAuthUserUseCase
import com.rossomak.flashcards.domain.usecase.SignOutUseCase
import com.rossomak.flashcards.testutil.MainDispatcherRule
import com.rossomak.flashcards.testutil.assertValue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentAuthUserUseCase: GetCurrentAuthUserUseCase = mockk()
    private val signOutUseCase: SignOutUseCase = mockk(relaxed = true)

    private val testUser = AuthUser(uid = "u1", email = "a@b.com", displayName = "Alex", photoUrl = null)

    private fun createViewModel(): MainViewModel =
        MainViewModel(getCurrentAuthUserUseCase, signOutUseCase)

    @Test
    fun `initial state has isLoading true before user loads`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser

        val viewModel = createViewModel()

        viewModel.state.value.isLoading shouldBe true
    }

    @Test
    fun `init with authenticated user emits loaded state with display name and photo`() = runTest(mainDispatcherRule.testDispatcher) {
        val user = testUser.copy(photoUrl = "http://p")
        coEvery { getCurrentAuthUserUseCase() } returns user

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.assertValue {
            isLoading shouldBe false
            displayName shouldBe user.displayName
            photoUrl shouldBe user.photoUrl
            error shouldBe null
            navigationDestination shouldBe null
        }
    }

    @Test
    fun `init with null user navigates to Login`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.navigationDestination shouldBe MainDestination.Login
    }

    @Test
    fun `isLoading remains true when navigating to Login due to null user`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.assertValue {
            isLoading shouldBe true
            navigationDestination shouldBe MainDestination.Login
        }
    }

    @Test
    fun `display name falls back to email when displayName is blank`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns AuthUser("u1", "a@b.com", "  ", null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.displayName shouldBe "a@b.com"
    }

    @Test
    fun `display name falls back to User when displayName and email are blank`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns AuthUser("u1", "", "", null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.displayName shouldBe "User"
    }

    @Test
    fun `display name falls back to User when both are null`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns AuthUser("u1", null, null, null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.displayName shouldBe "User"
    }

    @Test
    fun `onSignOutClick success navigates to Login`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser
        coEvery { signOutUseCase() } returns Unit

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSignOutClick()
        advanceUntilIdle()

        viewModel.state.value.navigationDestination shouldBe MainDestination.Login
        coVerify(exactly = 1) { signOutUseCase() }
    }

    @Test
    fun `onSignOutClick still navigates to Login when sign-out throws`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser
        coEvery { signOutUseCase() } throws RuntimeException("network")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSignOutClick()
        advanceUntilIdle()

        viewModel.state.value.navigationDestination shouldBe MainDestination.Login
    }

    @Test
    fun `loadUser exception leaves state as loading with no navigation destination`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } throws RuntimeException("crash")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.assertValue {
            isLoading shouldBe true
            navigationDestination shouldBe null
        }
    }

    @Test
    fun `onSignOutClick before loadUser completes still navigates to Login`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser

        val viewModel = createViewModel()
        viewModel.onSignOutClick()
        advanceUntilIdle()

        viewModel.state.value.navigationDestination shouldBe MainDestination.Login
    }

    @Test
    fun `onSignOutClick called twice invokes signOutUseCase twice`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { getCurrentAuthUserUseCase() } returns testUser

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSignOutClick()
        viewModel.onSignOutClick()
        advanceUntilIdle()

        coVerify(exactly = 2) { signOutUseCase() }
        viewModel.state.value.navigationDestination shouldBe MainDestination.Login
    }
}
