package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.UserPreferences
import com.rossomak.flashcards.core.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveUserPreferencesUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<UserPreferences> = repository.userPreferences()
}
