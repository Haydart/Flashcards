package com.rossomak.flashcards.core.data.source

import com.rossomak.flashcards.core.domain.model.StudySessionPreference
import com.rossomak.flashcards.core.domain.model.StudySessionPreferences
import kotlinx.coroutines.flow.Flow

interface StudySessionPreferencesLocalDataSource {

    fun studySessionPreferences(): Flow<StudySessionPreferences>

    suspend fun save(preference: StudySessionPreference)
}
