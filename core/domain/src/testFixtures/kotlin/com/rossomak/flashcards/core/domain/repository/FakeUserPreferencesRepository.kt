package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.StudyPreferences
import com.rossomak.flashcards.core.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserPreferencesRepository : UserPreferencesRepository {

    val preferences = MutableStateFlow(UserPreferences())

    /** Set to make [saveStudyPreferences] throw, so callers can exercise the failure path. */
    var saveStudyPreferencesError: Throwable? = null

    /** Records the order of writes, so tests can assert the flag is flipped only after a save. */
    val recordedCalls = mutableListOf<String>()

    override fun userPreferences(): Flow<UserPreferences> = preferences

    override suspend fun saveStudyPreferences(studyPreferences: StudyPreferences) {
        recordedCalls += SAVE_STUDY_PREFERENCES
        saveStudyPreferencesError?.let { throw it }
        preferences.value = preferences.value.copy(studyPreferences = studyPreferences)
    }

    override suspend fun setHasSeenOnboarding(hasSeenOnboarding: Boolean) {
        recordedCalls += SET_HAS_SEEN_ONBOARDING
        preferences.value = preferences.value.copy(hasSeenOnboarding = hasSeenOnboarding)
    }

    companion object {
        const val SAVE_STUDY_PREFERENCES = "saveStudyPreferences"
        const val SET_HAS_SEEN_ONBOARDING = "setHasSeenOnboarding"
    }
}
