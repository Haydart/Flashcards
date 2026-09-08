package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.SessionCommit
import kotlinx.coroutines.yield

class FakeStudySessionRepository : StudySessionRepository {
    var commitResultToReturn: Result<Unit> = Result.success(Unit)

    /** Simulates a later async rejection of an already-locally-accepted commit, delivered inline. */
    var rejectionToDeliver: Throwable? = null

    /** Every commit, in call order. */
    val committedSessionCommits: MutableList<SessionCommit> = mutableListOf()

    /**
     * [yield]s once before reporting anything back, mirroring the real repository's genuine
     * dispatcher hop (`withContext(Dispatchers.IO)`). Without it, a caller that fires this from
     * `init {}` and only subscribes to a result afterward (as [StudySessionRepository.commitSession]'s
     * `onRejected` callers do) would see this fake complete inline, before that subscription exists —
     * a timing this fake would otherwise get wrong relative to the real implementation.
     */
    override suspend fun commitSession(sessionCommit: SessionCommit, onRejected: (Throwable) -> Unit): Result<Unit> {
        committedSessionCommits.add(sessionCommit)
        yield()
        rejectionToDeliver?.let(onRejected)
        return commitResultToReturn
    }
}
