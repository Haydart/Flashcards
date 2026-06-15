package com.rossomak.flashcards.domain.usecase

import com.rossomak.flashcards.domain.model.AuthUser
import com.rossomak.flashcards.domain.repository.AuthRepository
import com.rossomak.flashcards.domain.usecase.base.NoParamUseCase
import javax.inject.Inject

class GetCurrentAuthUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : NoParamUseCase<AuthUser?> {

    override suspend operator fun invoke(): AuthUser? = authRepository.getCurrentUser()
}
