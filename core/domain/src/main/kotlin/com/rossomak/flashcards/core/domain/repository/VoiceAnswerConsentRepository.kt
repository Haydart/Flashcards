package com.rossomak.flashcards.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persists the user's explicit consent to background microphone listening during study
 * sessions — a Play Store policy requirement for background RECORD_AUDIO and part of the
 * feature's own privacy posture. Voice answering never starts without this consent recorded.
 */
interface VoiceAnswerConsentRepository {

    fun observeConsent(): Flow<Boolean>

    suspend fun setConsent(granted: Boolean)
}
