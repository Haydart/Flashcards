package com.rossomak.flashcards.core.data

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.rossomak.flashcards.core.data.worker.SessionSubmissionDeliveryWorker
import javax.inject.Inject

/**
 * The one place [SessionSubmissionDeliveryWorker] gets enqueued from (ticket 03). Two call sites
 * share this single class rather than each building their own [androidx.work.OneTimeWorkRequest]:
 * [com.rossomak.flashcards.core.data.repository.DefaultSessionSubmissionRepository] (right after
 * appending a freshly finished session to the local queue) and
 * [com.rossomak.flashcards.FlashcardsApplication] (once, unconditionally, on every app start — the
 * sole recovery path for a session queued by a previous process that never got to drain it). One
 * shared definition keeps those two call sites' work requests — constraints, backoff policy — from
 * silently drifting apart if only one of them were ever edited.
 *
 * [ExistingWorkPolicy.KEEP]: if a drain is already enqueued or running, [scheduleDrain] is a no-op.
 * This is safe, not lossy — the worker always re-reads the full pending list from
 * [com.rossomak.flashcards.core.data.source.PendingSessionSubmissionLocalDataSource] at the *start* of
 * its own run, so any entry appended before that read is picked up in the same run; an entry appended
 * after a run has already started its read simply waits for the *next* [scheduleDrain] call (the next
 * session finishing, or the next app start) — by then the previous run has completed and the unique
 * work slot is free again, so `KEEP` no longer blocks the new enqueue.
 */
class SessionSubmissionDrainScheduler @Inject constructor(
    private val workManager: WorkManager,
) {

    fun scheduleDrain() {
        val request = OneTimeWorkRequestBuilder<SessionSubmissionDeliveryWorker>().build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "session_submission_drain"
    }
}
