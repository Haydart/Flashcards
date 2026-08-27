package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.StudySessionPreference
import com.rossomak.flashcards.core.domain.repository.StudySessionPreferencesRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Returns a [Result] so callers can gate on the write succeeding — see [SaveUserPreferenceUseCase]
 * for the same reasoning.
 */
class SaveStudySessionPreferenceUseCase @Inject constructor(
    private val repository: StudySessionPreferencesRepository,
) : UseCase<StudySessionPreference, Result<Unit>> {

    override suspend operator fun invoke(params: StudySessionPreference): Result<Unit> =
        try {
            Result.success(repository.save(params))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
}
