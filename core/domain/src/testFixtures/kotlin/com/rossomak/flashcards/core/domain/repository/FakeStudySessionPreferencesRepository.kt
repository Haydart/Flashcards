package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.StudySessionPreference
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.DefaultStudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.RatedAttempts
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.ReadAloudEnabled
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.SessionLength
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.SortOrder
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.SubcategoryCountRange
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.VoiceAnsweringEnabled
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.VoicePlayback
import com.rossomak.flashcards.core.domain.model.StudySessionPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeStudySessionPreferencesRepository : StudySessionPreferencesRepository {

    val preferences = MutableStateFlow(StudySessionPreferences())

    /** Set to make [save] throw, so callers can exercise the failure path. */
    var saveError: Throwable? = null

    override fun studySessionPreferences(): Flow<StudySessionPreferences> = preferences

    override suspend fun save(preference: StudySessionPreference) {
        saveError?.let { throw it }
        preferences.value = when (preference) {
            is DefaultStudyMode -> preferences.value.copy(defaultStudyMode = preference.value)
            is VoiceAnsweringEnabled -> preferences.value.copy(voiceAnsweringEnabled = preference.value)
            is RatedAttempts -> preferences.value.copy(ratedAttempts = preference.value)
            is ReadAloudEnabled -> preferences.value.copy(readAloudEnabled = preference.value)
            is SessionLength -> preferences.value.copy(sessionLength = preference.value)
            is SortOrder -> preferences.value.copy(sortOrder = preference.value)
            is SubcategoryCountRange -> preferences.value.copy(subcategoryCountRange = preference.value)
            is VoicePlayback -> preferences.value.copy(voiceSettings = preference.value)
        }
    }
}
