package com.rossomak.flashcards.domain.usecase

import com.rossomak.flashcards.domain.repository.AuthRepository
import com.rossomak.flashcards.domain.usecase.base.NoParamUseCase
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : NoParamUseCase<Unit> {
    override suspend operator fun invoke() {
        authRepository.signOut()
    }
}
