package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.repository.UserPreferencesRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

class SetHasSeenOnboardingUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
) : UseCase<Boolean, Result<Unit>> {

    override suspend operator fun invoke(params: Boolean): Result<Unit> =
        runCatching { repository.setHasSeenOnboarding(params) }
}
