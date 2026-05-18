package com.rossomak.flashcards.presentation.login

import com.rossomak.flashcards.domain.model.AuthUser
import com.rossomak.flashcards.domain.repository.AuthRepository
import com.rossomak.flashcards.domain.usecase.SignInWithGoogleUseCase
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
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        viewModel = LoginViewModel(SignInWithGoogleUseCase(authRepository))
    }

    @Test
    fun `onSignInStarted sets isSigningIn true and clears error`() {
        viewModel.onSignInStarted()

        viewModel.state.assertValue {
            isSigningIn shouldBe true
            errorMessage shouldBe null
        }
    }

    @Test
    fun `onGoogleIdTokenReceived success navigates to Main and clears error`() = runTest(mainDispatcherRule.testDispatcher) {
        val token = "token"
        val user = AuthUser(uid = "u1", email = "a@b.com", displayName = "Alex", photoUrl = null)
        coEvery { authRepository.signInWithGoogleIdToken(token) } returns Result.success(user)

        viewModel.onGoogleIdTokenReceived(token)
        advanceUntilIdle()

        viewModel.state.assertValue {
            isSigningIn shouldBe false
            errorMessage shouldBe null
            navigationDestination shouldBe LoginDestination.Main
        }
        coVerify(exactly = 1) { authRepository.signInWithGoogleIdToken(token) }
    }

    @Test
    fun `onGoogleIdTokenReceived failure surfaces exception message`() = runTest(mainDispatcherRule.testDispatcher) {
        val expectedError = "boom"
        coEvery { authRepository.signInWithGoogleIdToken(any()) } returns Result.failure(RuntimeException(expectedError))

        viewModel.onGoogleIdTokenReceived("token")
        advanceUntilIdle()

        viewModel.state.assertValue {
            isSigningIn shouldBe false
            errorMessage shouldBe expectedError
            navigationDestination shouldBe null
        }
    }

    @Test
    fun `onGoogleIdTokenReceived failure with null message falls back to default`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { authRepository.signInWithGoogleIdToken(any()) } returns Result.failure(RuntimeException(null as String?))

        viewModel.onGoogleIdTokenReceived("token")
        advanceUntilIdle()

        viewModel.state.value.errorMessage shouldBe "Sign-in failed"
    }

    @Test
    fun `onSignInFailed with message sets error and stops signing in`() {
        val expectedError = "network down"
        viewModel.onSignInStarted()

        viewModel.onSignInFailed(expectedError)

        viewModel.state.assertValue {
            isSigningIn shouldBe false
            errorMessage shouldBe expectedError
        }
    }

    @Test
    fun `onSignInFailed with null falls back to default message`() {
        viewModel.onSignInFailed(null)

        viewModel.state.value.errorMessage shouldBe "Sign-in failed"
    }
}
