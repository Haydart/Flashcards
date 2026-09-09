package com.rossomak.flashcards.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.rossomak.flashcards.core.data.model.PendingSessionSubmissionMapper.toDomain
import com.rossomak.flashcards.core.data.repository.RemoteSessionSubmissionRepository
import com.rossomak.flashcards.core.data.source.PendingSessionSubmissionLocalDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Drains the pending-session-submission queue (ticket 03). See
 * [com.rossomak.flashcards.core.data.repository.DefaultSessionSubmissionRepository]'s class doc for
 * the append side of this same queue, and
 * [com.rossomak.flashcards.core.data.source.FilePendingSessionSubmissionLocalDataSource]'s for the
 * local store's shape and concurrency guarantee. Scheduled exclusively via
 * [com.rossomak.flashcards.core.data.SessionSubmissionDrainScheduler] — never constructed or enqueued
 * any other way.
 *
 * [doWork] reads every entry [localDataSource] currently holds, sorts it FIFO by
 * [com.rossomak.flashcards.core.domain.model.SessionResult.startedAt] (oldest first), and submits
 * each one in turn to [remoteSessionSubmissionRepository] — the concrete, network-only repository,
 * constructor-injected by concrete type rather than through the
 * [com.rossomak.flashcards.core.domain.repository.SessionSubmissionRepository] interface, so this
 * worker can never accidentally re-enqueue what it is itself draining. FIFO ordering is a
 * plausibility, not a correctness, requirement: the mastery/demastery/defense-bonus outcomes the
 * `submitStudySession` function computes read the account's *current* state at commit time, so
 * delivering an earlier-started session before a later one keeps those outcomes closer to what they
 * would have been if both had been submitted live.
 *
 * On any entry's delivery failure, [doWork] stops immediately and returns [Result.retry] — the failed
 * entry and everything after it in this run stay queued, untouched. No bespoke retry/backoff logic
 * lives here: [androidx.work.WorkManager]'s own retry policy on the enqueued
 * [androidx.work.OneTimeWorkRequest] governs when the next attempt happens. A retried run re-reads
 * [localDataSource] from scratch, so an entry already cleared by a successful earlier attempt is
 * never resubmitted; an entry that *was* actually delivered to the function on a previous attempt but
 * failed to clear locally before a crash gets resent on the next run — harmless, since
 * `submitStudySession` is idempotent per session id.
 *
 * Recovery after the app (or the process WorkManager was running in) is killed mid-drain needs no
 * separate code path: [com.rossomak.flashcards.FlashcardsApplication] unconditionally calls
 * [com.rossomak.flashcards.core.data.SessionSubmissionDrainScheduler.scheduleDrain] on every app
 * start, and `ExistingWorkPolicy.KEEP` makes that call a safe no-op if a drain is already pending —
 * this worker's next run, whenever it happens, always starts by reading the file fresh.
 */
@HiltWorker
class SessionSubmissionDeliveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val remoteSessionSubmissionRepository: RemoteSessionSubmissionRepository,
    private val localDataSource: PendingSessionSubmissionLocalDataSource,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val pendingEntries = localDataSource.listAll().sortedBy { it.startedAtEpochMillis }
        for (entry in pendingEntries) {
            val submissionResult = remoteSessionSubmissionRepository.submitSession(entry.toDomain())
            if (submissionResult.isFailure) return Result.retry()
            localDataSource.remove(entry.id)
        }
        return Result.success()
    }
}
