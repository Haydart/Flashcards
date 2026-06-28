package com.rossomak.flashcards.domain.usecase

import com.rossomak.flashcards.core.domain.model.AuthUser
import com.rossomak.flashcards.core.domain.repository.AuthRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SignInWithGoogleUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val useCase = SignInWithGoogleUseCase(authRepository)

    @Test
    fun `forwards token to repository and returns success result`() = runTest {
        val token = "token-xyz"
        val user = AuthUser(uid = "u1", email = "a@b.com", displayName = "Alex", photoUrl = null)
        coEvery { authRepository.signInWithGoogleIdToken(token) } returns Result.success(user)

        val result = useCase(token)

        result.isSuccess shouldBe true
        result.getOrNull() shouldBe user
        coVerify(exactly = 1) { authRepository.signInWithGoogleIdToken(token) }
    }

    @Test
    fun `forwards failure result unchanged`() = runTest {
        val error = IllegalStateException("bad token")
        coEvery { authRepository.signInWithGoogleIdToken(any()) } returns Result.failure(error)

        val result = useCase("anything")

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { authRepository.signInWithGoogleIdToken("anything") }
    }
}
