package com.rossomak.flashcards.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerConsentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultVoiceAnswerConsentRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : VoiceAnswerConsentRepository {

    override fun observeConsent(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[VOICE_ANSWER_CONSENT_KEY] ?: false }

    override suspend fun setConsent(granted: Boolean) {
        dataStore.edit { preferences -> preferences[VOICE_ANSWER_CONSENT_KEY] = granted }
    }

    private companion object {
        val VOICE_ANSWER_CONSENT_KEY = booleanPreferencesKey("voice_answer_consent")
    }
}
