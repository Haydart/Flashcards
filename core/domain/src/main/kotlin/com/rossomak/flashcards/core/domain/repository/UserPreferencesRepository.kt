package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.StudyPreferences
import com.rossomak.flashcards.core.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * [setHasSeenOnboarding] is intentionally separate from [saveStudyPreferences] so onboarding can
 * commit the user's choices first and only then mark the flow complete — the flag doubles as a
 * "everything was written" gate.
 */
interface UserPreferencesRepository {

    fun userPreferences(): Flow<UserPreferences>

    suspend fun saveStudyPreferences(studyPreferences: StudyPreferences)

    suspend fun setHasSeenOnboarding(hasSeenOnboarding: Boolean)
}
