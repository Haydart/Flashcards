package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.AuthUser
import com.rossomak.flashcards.core.domain.repository.AuthRepository
import com.rossomak.flashcards.core.domain.usecase.base.NoParamUseCase
import javax.inject.Inject

class GetCurrentAuthUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : NoParamUseCase<AuthUser?> {

    override suspend operator fun invoke(): AuthUser? = authRepository.getCurrentUser()
}
