package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.source.VoiceAnswerConsentLocalDataSource
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerConsentRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultVoiceAnswerConsentRepository @Inject constructor(
    private val localDataSource: VoiceAnswerConsentLocalDataSource,
) : VoiceAnswerConsentRepository {

    override fun observeConsent(): Flow<Boolean> = localDataSource.observeConsent()

    override suspend fun setConsent(granted: Boolean) = localDataSource.setConsent(granted)
}
