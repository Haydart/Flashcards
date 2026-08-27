package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.StudySessionPreference
import com.rossomak.flashcards.core.domain.model.StudySessionPreferences
import kotlinx.coroutines.flow.Flow

interface StudySessionPreferencesRepository {

    fun studySessionPreferences(): Flow<StudySessionPreferences>

    suspend fun save(preference: StudySessionPreference)
}
