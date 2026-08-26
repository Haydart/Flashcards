package com.rossomak.flashcards.core.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rossomak.flashcards.core.data.di.VoiceDataStore
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreVoiceSettingsLocalDataSource @Inject constructor(
    @VoiceDataStore private val dataStore: DataStore<Preferences>,
) : VoiceSettingsLocalDataSource {

    override fun voiceSettings(): Flow<VoiceSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            VoiceSettings(
                speechRate = prefs[SPEECH_RATE_KEY] ?: DEFAULT_SPEECH_RATE,
                voiceId = prefs[VOICE_ID_KEY],
            )
        }

    override suspend fun save(settings: VoiceSettings) {
        dataStore.edit { prefs ->
            prefs[SPEECH_RATE_KEY] = settings.speechRate
            val voiceId = settings.voiceId
            if (voiceId != null) {
                prefs[VOICE_ID_KEY] = voiceId
            } else {
                prefs.remove(VOICE_ID_KEY)
            }
        }
    }

    private companion object {
        val DEFAULT_SPEECH_RATE = VoiceSettings().speechRate
        val SPEECH_RATE_KEY = floatPreferencesKey("voice_speech_rate")
        val VOICE_ID_KEY = stringPreferencesKey("voice_id")
    }
}
