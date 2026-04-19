package com.rossomak.flashcards.domain.usecase

import com.rossomak.flashcards.domain.repository.UserRepository
import com.rossomak.flashcards.domain.usecase.base.NoParamUseCase
import javax.inject.Inject

class GetUserNameUseCase @Inject constructor(
    private val userRepository: UserRepository
) : NoParamUseCase<Result<String>> {

    override suspend fun invoke(): Result<String> {
        return userRepository.getCurrentUser().map { it.name }
    }
}
