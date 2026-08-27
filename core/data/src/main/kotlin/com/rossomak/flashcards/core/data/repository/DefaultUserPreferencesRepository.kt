package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.source.UserPreferencesLocalDataSource
import com.rossomak.flashcards.core.domain.model.UserPreference
import com.rossomak.flashcards.core.domain.model.UserPreferences
import com.rossomak.flashcards.core.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultUserPreferencesRepository @Inject constructor(
    private val localDataSource: UserPreferencesLocalDataSource,
) : UserPreferencesRepository {

    override fun userPreferences(): Flow<UserPreferences> = localDataSource.userPreferences()

    override suspend fun save(preference: UserPreference) = localDataSource.save(preference)
}
