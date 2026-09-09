package com.rossomak.flashcards.core.data.source

import android.content.Context
import android.util.Log
import com.rossomak.flashcards.core.data.model.PendingSessionSubmissionDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * File-backed [PendingSessionSubmissionLocalDataSource]: a single JSON array, [FILE_NAME] under
 * [Context.filesDir], holding every not-yet-delivered [PendingSessionSubmissionDto]. Not Room, not a
 * DataStore entry — a flat file is enough for this shape: no cap is enforced on how many entries it
 * can hold, on the expectation that it stays small (this is a flashcards app, not an offline-first
 * sync tool), and the whole list is read/rewritten wholesale on every mutation rather than indexed.
 *
 * **Concurrency**: [append] (called from [com.rossomak.flashcards.core.data.repository.DefaultSessionSubmissionRepository],
 * potentially from two sessions finishing seconds apart) and [listAll]/[remove] (called from
 * [com.rossomak.flashcards.core.data.worker.SessionSubmissionDeliveryWorker]'s drain loop, on its own
 * dispatcher) can run concurrently against the same file. Every operation below is serialized through
 * one [mutex], held for the full read-modify-write — without it, an interleaved write risks a lost
 * update or a corrupted JSON array.
 *
 * A file that fails to parse (corrupted, or simply absent on first run) is treated as an empty queue
 * rather than a crash — [readAll] logs the failure non-fatally and returns `emptyList()`. This mirrors
 * [DataStoreStudySessionPreferencesLocalDataSource]'s own `IOException` fallback: a local cache that
 * can't be read is not worth crashing a launch over, and a genuinely lost entry here is not silent —
 * ticket 03 accepts that a corrupted file drops its queued sessions, since nothing else can recover
 * them either.
 *
 * **App-start recovery**: this class has no init-time logic of its own. [com.rossomak.flashcards.FlashcardsApplication]
 * unconditionally re-enqueues the drain worker on every app start (via
 * [com.rossomak.flashcards.core.data.SessionSubmissionDrainScheduler]), and that worker always starts
 * by reading whatever this file currently holds — so a session queued by a process that got killed
 * before draining is picked up the next time the app runs, with no separate "check for leftover
 * records" step.
 */
class FilePendingSessionSubmissionLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : PendingSessionSubmissionLocalDataSource {

    private val mutex = Mutex()
    private val file: File get() = File(context.filesDir, FILE_NAME)

    override suspend fun append(pendingSessionSubmission: PendingSessionSubmissionDto) = withContext(Dispatchers.IO) {
        mutex.withLock {
            writeAll(readAll() + pendingSessionSubmission)
        }
    }

    override suspend fun listAll(): List<PendingSessionSubmissionDto> = withContext(Dispatchers.IO) {
        mutex.withLock { readAll() }
    }

    override suspend fun remove(sessionId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            writeAll(readAll().filterNot { it.id == sessionId })
        }
    }

    /** Must only be called while holding [mutex]. */
    private fun readAll(): List<PendingSessionSubmissionDto> {
        if (!file.exists()) return emptyList()
        return try {
            Json.decodeFromString<List<PendingSessionSubmissionDto>>(file.readText())
        } catch (exception: SerializationException) {
            Log.e(TAG, "Pending session submission queue file is corrupted, treating it as empty", exception)
            emptyList()
        }
    }

    /** Must only be called while holding [mutex]. */
    private fun writeAll(entries: List<PendingSessionSubmissionDto>) {
        file.writeText(Json.encodeToString(entries))
    }

    private companion object {
        const val TAG = "PendingSessionQueue"
        const val FILE_NAME = "pending_session_submissions.json"
    }
}
