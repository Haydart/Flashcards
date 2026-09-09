package com.rossomak.flashcards.core.data.source

import com.rossomak.flashcards.core.data.model.PendingSessionSubmissionDto

/**
 * The local durable delivery queue's read/write surface — matches this codebase's
 * `XxxLocalDataSource` convention (cf. [StudySessionPreferencesLocalDataSource] /
 * [DataStoreStudySessionPreferencesLocalDataSource]), extended here to a list-shaped store: every
 * other local data source in this app is DataStore's key-value Preferences, this is the first one
 * holding an ordered collection of pending records.
 *
 * [com.rossomak.flashcards.core.data.repository.DefaultSessionSubmissionRepository] is the only
 * [append] caller; [com.rossomak.flashcards.core.data.worker.SessionSubmissionDeliveryWorker] is the
 * only [listAll]/[remove] caller. See [FilePendingSessionSubmissionLocalDataSource] for the concrete
 * storage shape and its concurrency guarantee across those two callers.
 */
interface PendingSessionSubmissionLocalDataSource {

    suspend fun append(pendingSessionSubmission: PendingSessionSubmissionDto)

    suspend fun listAll(): List<PendingSessionSubmissionDto>

    suspend fun remove(sessionId: String)
}
