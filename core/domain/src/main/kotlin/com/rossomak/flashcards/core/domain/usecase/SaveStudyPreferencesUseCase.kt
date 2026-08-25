package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.StudyPreferences
import com.rossomak.flashcards.core.domain.repository.UserPreferencesRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

/**
 * Returns a [Result] so callers can gate on the write succeeding. Local storage rarely fails
 * today, but onboarding only flips its completion flag after this succeeds, so the outcome has to
 * be observable rather than swallowed.
 */
class SaveStudyPreferencesUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
) : UseCase<StudyPreferences, Result<Unit>> {

    override suspend operator fun invoke(params: StudyPreferences): Result<Unit> =
        runCatching { repository.saveStudyPreferences(params) }
}
