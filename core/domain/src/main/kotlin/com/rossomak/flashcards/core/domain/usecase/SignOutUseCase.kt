package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.repository.AuthRepository
import com.rossomak.flashcards.core.domain.usecase.base.NoParamUseCase
import javax.inject.Inject

class SignOutUseCase @Inject constructor(private val authRepository: AuthRepository) : NoParamUseCase<Unit> {

    override suspend operator fun invoke() {
        authRepository.signOut()
    }
}
