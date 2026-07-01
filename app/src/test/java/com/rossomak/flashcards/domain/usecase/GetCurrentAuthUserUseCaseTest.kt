package com.rossomak.flashcards.domain.usecase

import com.rossomak.flashcards.core.domain.model.AuthUser
import com.rossomak.flashcards.core.domain.repository.AuthRepository
import com.rossomak.flashcards.core.domain.usecase.GetCurrentAuthUserUseCase
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetCurrentAuthUserUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val useCase = GetCurrentAuthUserUseCase(authRepository)

    @Test
    fun `returns user when repository has authenticated user`() = runTest {
        val user = AuthUser(uid = "u1", email = "a@b.com", displayName = "Alex", photoUrl = null)
        every { authRepository.getCurrentUser() } returns user

        val result = useCase()

        result shouldBe user
        verify(exactly = 1) { authRepository.getCurrentUser() }
    }

    @Test
    fun `returns null when repository has no user`() = runTest {
        every { authRepository.getCurrentUser() } returns null

        val result = useCase()

        result shouldBe null
        verify(exactly = 1) { authRepository.getCurrentUser() }
    }
}
