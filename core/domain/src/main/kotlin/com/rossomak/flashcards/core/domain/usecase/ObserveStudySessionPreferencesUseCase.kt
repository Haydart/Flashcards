package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.StudySessionPreferences
import com.rossomak.flashcards.core.domain.repository.StudySessionPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveStudySessionPreferencesUseCase @Inject constructor(
    private val repository: StudySessionPreferencesRepository,
) {
    operator fun invoke(): Flow<StudySessionPreferences> = repository.studySessionPreferences()
}
