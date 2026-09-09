package com.rossomak.flashcards.core.data.source

import android.content.Context
import android.util.Log
import com.rossomak.flashcards.core.data.model.PendingSessionSubmissionDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
 * File-backed [PendingSessionSubmissionLocalDataSource]: [FILE_NAME] under [Context.filesDir], one
 * [PendingSessionSubmissionDto] JSON-encoded per line (JSONL), holding every not-yet-delivered entry.
 * Not Room, not a DataStore entry — a flat file is enough for this shape: no cap is enforced on how
 * many entries it can hold, on the expectation that it stays small (this is a flashcards app, not an
 * offline-first sync tool).
 *
 * **Why one entry per line, not a single JSON array**: a single malformed entry — hand-edited, or
 * corrupted by a future format change — must never take the rest of the queue down with it. With
 * line-per-entry, [readAll] parses each line independently: a line that fails to decode is logged and
 * skipped, every other line's entry survives untouched, and the bad line disappears from disk on the
 * very next mutation (since [append]/[remove] both persist exactly what [readAll] returned, minus/plus
 * one entry — a skipped line was never in that result to begin with). A whole-file read failure (the
 * file itself cannot be opened or read at all, e.g. an [IOException] off the raw file access) is a
 * different failure — nothing can be salvaged line-by-line if the file can't be read in the first
 * place — and is still treated as an empty queue rather than a crash: a local cache that can't be read
 * is not worth crashing a launch over, mirroring [DataStoreStudySessionPreferencesLocalDataSource]'s
 * own `IOException` fallback.
 *
 * **Concurrency**: [append] (called from [com.rossomak.flashcards.core.data.repository.DefaultSessionSubmissionRepository],
 * potentially from two sessions finishing seconds apart) and [listAll]/[remove] (called from
 * [com.rossomak.flashcards.core.data.worker.SessionSubmissionDeliveryWorker]'s drain loop, on its own
 * dispatcher) can run concurrently against the same file. Every operation below is serialized through
 * one [mutex] — without it, an interleaved write risks a lost update or a torn line.
 *
 * [append] writes incrementally: it opens [file] in append mode and writes just the new entry as one
 * more line, without reading the rest of the queue first — cheap, and it means a queue with an
 * unreadable *backlog* still accepts new sessions; only draining/reading the backlog is affected.
 * [remove] has to drop one line out of the middle, so it still reads every entry, filters, and rewrites
 * the whole file. [writeAll] (used by [remove], and by nothing else) writes to a sibling temp file
 * first, then atomically renames it over [file]: a process death mid-write leaves either the old
 * complete file or the new complete file on disk, never a half-written one. A process death mid-[append]
 * can leave a torn trailing line — [readAll]'s per-line skip handles that the same way it handles any
 * other corrupted line.
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
            FileOutputStream(file, true).bufferedWriter().use { writer ->
                writer.write(Json.encodeToString(pendingSessionSubmission))
                writer.newLine()
            }
            Log.d(TAG, "Appended session ${pendingSessionSubmission.id} to queue file")
            Unit
        }
    }

    override suspend fun listAll(): List<PendingSessionSubmissionDto> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readAll().also { Log.d(TAG, "Read queue file: ${it.size} entries") }
        }
    }

    override suspend fun remove(sessionId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = readAll().filterNot { it.id == sessionId }
            writeAll(updated)
            Log.d(TAG, "Removed session $sessionId from queue file, ${updated.size} entries remain")
            Unit
        }
    }

    /**
     * Must only be called while holding [mutex]. A line that fails to decode is logged and dropped —
     * it never reaches the returned list, so it is gone from disk too as soon as [append] or [remove]
     * next persists the result. A whole-file [IOException] (the file can't be read at all) is treated
     * as an empty queue instead, since nothing can be recovered line-by-line in that case.
     */
    private fun readAll(): List<PendingSessionSubmissionDto> {
        if (!file.exists()) return emptyList()
        val lines = try {
            file.readLines()
        } catch (exception: IOException) {
            Log.e(TAG, "Pending session submission queue file could not be read, treating it as empty", exception)
            return emptyList()
        }
        return lines
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                try {
                    Json.decodeFromString<PendingSessionSubmissionDto>(line)
                } catch (exception: SerializationException) {
                    Log.e(TAG, "Skipping one corrupted pending session submission queue line", exception)
                    null
                }
            }
    }

    /** Must only be called while holding [mutex]. Atomic: never leaves [file] half-written. */
    private fun writeAll(entries: List<PendingSessionSubmissionDto>) {
        val tempFile = File(context.filesDir, "$FILE_NAME.tmp")
        // Every line, including the last, ends with a newline: append() opens the file in append mode
        // and must always start writing on a fresh line, never tack a new entry onto an unterminated one.
        tempFile.writeText(entries.joinToString(separator = "") { "${Json.encodeToString(it)}\n" })
        if (!tempFile.renameTo(file)) {
            // Same filesystem, same directory — practically always succeeds; this is a hard failure
            // if it doesn't, since the caller's mutation would otherwise silently vanish.
            error("Failed to atomically replace $FILE_NAME with its updated contents")
        }
    }

    private companion object {
        const val TAG = "PendingSessionQueue"
        const val FILE_NAME = "pending_session_submissions.jsonl"
    }
}
