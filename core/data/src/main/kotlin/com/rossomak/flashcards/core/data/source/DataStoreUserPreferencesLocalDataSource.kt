package com.rossomak.flashcards.core.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.rossomak.flashcards.core.data.di.UserPreferencesDataStore
import com.rossomak.flashcards.core.domain.model.DailyGoal
import com.rossomak.flashcards.core.domain.model.UserPreference
import com.rossomak.flashcards.core.domain.model.UserPreference.DailyGoalMinutes
import com.rossomak.flashcards.core.domain.model.UserPreference.HasSeenOnboarding
import com.rossomak.flashcards.core.domain.model.UserPreferences
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreUserPreferencesLocalDataSource @Inject constructor(
    @UserPreferencesDataStore private val dataStore: DataStore<Preferences>,
) : UserPreferencesLocalDataSource {

    override fun userPreferences(): Flow<UserPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            UserPreferences(
                hasSeenOnboarding = prefs[HAS_SEEN_ONBOARDING_KEY] ?: DEFAULT_HAS_SEEN_ONBOARDING,
                dailyGoalMinutes = prefs[DAILY_GOAL_MINUTES_KEY] ?: DailyGoal.DEFAULT_MINUTES,
            )
        }

    override suspend fun save(preference: UserPreference) {
        dataStore.edit { prefs ->
            when (preference) {
                is DailyGoalMinutes -> prefs[DAILY_GOAL_MINUTES_KEY] = DailyGoal.coerce(preference.value)
                is HasSeenOnboarding -> prefs[HAS_SEEN_ONBOARDING_KEY] = preference.value
            }
        }
    }

    private companion object {
        val DEFAULT_HAS_SEEN_ONBOARDING = UserPreferences().hasSeenOnboarding
        val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
        val DAILY_GOAL_MINUTES_KEY = intPreferencesKey("daily_goal_minutes")
    }
}
