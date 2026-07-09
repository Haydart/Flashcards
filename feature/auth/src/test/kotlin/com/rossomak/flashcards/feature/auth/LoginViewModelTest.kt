package com.rossomak.flashcards.feature.auth

import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.model.AuthUser
import com.rossomak.flashcards.core.domain.repository.FakeAuthRepository
import com.rossomak.flashcards.core.domain.usecase.SignInWithGoogleUseCase
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()

    private val testUser = AuthUser("u1", "a@b.com", "Alex", null)

    private fun createViewModel(): LoginViewModel = LoginViewModel(SignInWithGoogleUseCase(authRepository))

    @Test
    fun `onGoogleIdTokenReceived with successful sign-in emits Main`() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInResult = Result.success(testUser)

        val viewModel = createViewModel()
        viewModel.onGoogleIdTokenReceived("token")

        viewModel.events.test {
            awaitItem() shouldBe LoginDestination.Main
        }
    }

    @Test
    fun `onGoogleIdTokenReceived with failed sign-in emits no navigation event`() = runTest(mainDispatcherRule.testDispatcher) {
        val errorMessage = "network down"
        authRepository.signInResult = Result.failure(IllegalStateException(errorMessage))

        val viewModel = createViewModel()
        viewModel.onGoogleIdTokenReceived("token")
        advanceUntilIdle()

        viewModel.state.value.errorMessage shouldBe errorMessage
        viewModel.events.test {
            expectNoEvents()
        }
    }

    @Test
    fun `onSignInFailed emits no navigation event`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onSignInFailed("cancelled")
        advanceUntilIdle()

        viewModel.events.test {
            expectNoEvents()
        }
    }
}
