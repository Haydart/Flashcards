package com.rossomak.flashcards.domain.usecase

import com.rossomak.flashcards.core.domain.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SignOutUseCaseTest {

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val useCase = SignOutUseCase(authRepository)

    @Test
    fun `delegates to authRepository signOut exactly once`() = runTest {
        every { authRepository.signOut() } returns Unit

        useCase()

        verify(exactly = 1) { authRepository.signOut() }
    }
}
