package com.rossomak.flashcards.core.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreVoiceAnswerConsentLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : VoiceAnswerConsentLocalDataSource {

    override fun observeConsent(): Flow<Boolean> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[VOICE_ANSWER_CONSENT_KEY] ?: false }

    override suspend fun setConsent(granted: Boolean) {
        dataStore.edit { preferences -> preferences[VOICE_ANSWER_CONSENT_KEY] = granted }
    }

    private companion object {
        val VOICE_ANSWER_CONSENT_KEY = booleanPreferencesKey("voice_answer_consent")
    }
}
