package com.rossomak.flashcards.core.data.worker

import android.content.Context
import android.util.Log
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
 * On any entry's delivery failure, [doWork] stops that entry's run and returns [Result.retry] — the
 * failed entry and everything after it in this run stay queued, untouched. No bespoke backoff timer
 * lives here: [androidx.work.WorkManager]'s own retry/backoff policy on the enqueued
 * [androidx.work.OneTimeWorkRequest] governs when the next attempt happens. A retried run re-reads
 * [localDataSource] from scratch, so an entry already cleared by a successful earlier attempt is
 * never resubmitted; an entry that *was* actually delivered to the function on a previous attempt but
 * failed to clear locally before a crash gets resent on the next run — harmless, since
 * `submitStudySession` is idempotent per session id.
 *
 * Retries are bounded by [runAttemptCount] against [MAX_DELIVERY_ATTEMPTS], but only for the *head*
 * entry of a run: [runAttemptCount] counts this enqueued request's own attempts, not any individual
 * entry's — when every entry fails for the same shared-cause reason (backend outage, rejected auth
 * token), every entry hits the bound on the same attempt, and only the head one has actually been
 * retried that many times. Once the head is exhausted, [doWork] drops that one entry — logging it as a
 * permanent failure rather than silently losing it — and moves on to the rest of the queue in the
 * *same* run, instead of retrying it forever and blocking every entry behind it. Every later entry in
 * that same run still returns [Result.retry] on failure, no matter [runAttemptCount], so a shared-cause
 * outage cannot drop more than one entry per exhausted run. A fresh entry appended later resets the
 * count for itself: it isn't the same request attempt, and [ExistingWorkPolicy][androidx.work.ExistingWorkPolicy.KEEP]
 * only reuses the still-running/enqueued request, never a completed one.
 *
 * A queue entry that fails to convert back to a domain [com.rossomak.flashcards.core.domain.model.SessionResult]
 * (unknown `mode`/`state`, or a `Rated` card result missing `attemptsUsed`/`wasPreviouslyMastered`) is
 * malformed beyond repair, not a delivery failure: [doWork] catches that conversion, logs the invalid
 * entry, removes it from the queue, and continues to the next entry rather than stalling every entry
 * behind it or retrying something that can never succeed.
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
        Log.d(TAG, "Drain started: ${pendingEntries.size} pending entr${if (pendingEntries.size == 1) "y" else "ies"} (attempt ${runAttemptCount + 1})")
        pendingEntries.forEachIndexed { index, entry ->
            Log.d(TAG, "Submitting session ${entry.id} (${index + 1}/${pendingEntries.size})")
            val domainSessionResult = try {
                entry.toDomain()
            } catch (exception: IllegalArgumentException) {
                Log.e(TAG, "Session ${entry.id} is malformed and cannot be converted, dropping it from the queue", exception)
                localDataSource.remove(entry.id)
                return@forEachIndexed
            }
            val submissionResult = remoteSessionSubmissionRepository.submitSession(domainSessionResult)
            if (submissionResult.isFailure) {
                if (index == 0 && runAttemptCount + 1 >= MAX_DELIVERY_ATTEMPTS) {
                    Log.e(
                        TAG,
                        "Session ${entry.id} failed to deliver after $MAX_DELIVERY_ATTEMPTS attempts, " +
                            "dropping it from the queue so later entries can proceed",
                        submissionResult.exceptionOrNull(),
                    )
                    localDataSource.remove(entry.id)
                    return@forEachIndexed
                }
                Log.w(
                    TAG,
                    "Submission failed for session ${entry.id} (attempt ${runAttemptCount + 1}/$MAX_DELIVERY_ATTEMPTS), stopping drain and returning retry",
                    submissionResult.exceptionOrNull(),
                )
                return Result.retry()
            }
            localDataSource.remove(entry.id)
            Log.d(TAG, "Session ${entry.id} delivered and removed from queue")
        }
        Log.d(TAG, "Drain finished: all ${pendingEntries.size} entr${if (pendingEntries.size == 1) "y" else "ies"} resolved")
        return Result.success()
    }

    private companion object {
        const val TAG = "SessionSubmissionDrainWorker"

        /** See this class's own doc for why the attempt bound only ever applies to the head entry of a run. */
        const val MAX_DELIVERY_ATTEMPTS = 5
    }
}
