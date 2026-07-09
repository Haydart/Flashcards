package com.rossomak.flashcards.core.data.source

import kotlinx.coroutines.flow.Flow

interface VoiceAnswerConsentLocalDataSource {

    fun observeConsent(): Flow<Boolean>

    suspend fun setConsent(granted: Boolean)
}
