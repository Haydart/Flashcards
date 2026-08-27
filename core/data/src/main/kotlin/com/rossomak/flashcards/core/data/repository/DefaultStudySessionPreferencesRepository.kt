package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.source.StudySessionPreferencesLocalDataSource
import com.rossomak.flashcards.core.domain.model.StudySessionPreference
import com.rossomak.flashcards.core.domain.model.StudySessionPreferences
import com.rossomak.flashcards.core.domain.repository.StudySessionPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultStudySessionPreferencesRepository @Inject constructor(
    private val localDataSource: StudySessionPreferencesLocalDataSource,
) : StudySessionPreferencesRepository {

    override fun studySessionPreferences(): Flow<StudySessionPreferences> = localDataSource.studySessionPreferences()

    override suspend fun save(preference: StudySessionPreference) = localDataSource.save(preference)
}
