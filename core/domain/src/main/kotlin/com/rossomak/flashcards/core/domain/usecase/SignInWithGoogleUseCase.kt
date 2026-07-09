package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.AuthUser
import com.rossomak.flashcards.core.domain.repository.AuthRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(private val authRepository: AuthRepository) : UseCase<String, Result<AuthUser>> {

    override suspend operator fun invoke(params: String): Result<AuthUser> = authRepository.signInWithGoogleIdToken(idToken = params)
}
