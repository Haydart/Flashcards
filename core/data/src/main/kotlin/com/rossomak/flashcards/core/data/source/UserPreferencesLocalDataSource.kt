package com.rossomak.flashcards.core.data.source

import com.rossomak.flashcards.core.domain.model.UserPreference
import com.rossomak.flashcards.core.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesLocalDataSource {

    fun userPreferences(): Flow<UserPreferences>

    suspend fun save(preference: UserPreference)
}
