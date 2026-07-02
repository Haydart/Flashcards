package com.rossomak.flashcards.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import com.rossomak.flashcards.core.domain.repository.VoiceSettingsRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DefaultVoiceSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : VoiceSettingsRepository {

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

    companion object {
        private val DEFAULT_SPEECH_RATE = VoiceSettings().speechRate
        private val SPEECH_RATE_KEY = floatPreferencesKey("voice_speech_rate")
        private val VOICE_ID_KEY = stringPreferencesKey("voice_id")
    }
}
