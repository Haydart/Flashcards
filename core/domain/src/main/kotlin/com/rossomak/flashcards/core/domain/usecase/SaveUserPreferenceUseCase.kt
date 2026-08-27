package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.UserPreference
import com.rossomak.flashcards.core.domain.repository.UserPreferencesRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Returns a [Result] so callers can gate on the write succeeding. Local storage rarely fails
 * today, but onboarding only flips its completion flag after this succeeds, so the outcome has to
 * be observable rather than swallowed.
 */
class SaveUserPreferenceUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
) : UseCase<UserPreference, Result<Unit>> {

    override suspend operator fun invoke(params: UserPreference): Result<Unit> =
        try {
            Result.success(repository.save(params))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
}
