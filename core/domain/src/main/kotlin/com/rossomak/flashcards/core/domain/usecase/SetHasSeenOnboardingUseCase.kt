package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.repository.UserPreferencesRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class SetHasSeenOnboardingUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
) : UseCase<Boolean, Result<Unit>> {

    override suspend operator fun invoke(params: Boolean): Result<Unit> =
        try {
            Result.success(repository.setHasSeenOnboarding(params))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
}
