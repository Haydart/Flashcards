package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.SessionResult
import kotlinx.coroutines.yield

class FakeSessionSubmissionRepository : SessionSubmissionRepository {
    var resultToReturn: Result<Unit> = Result.success(Unit)

    /** Every submitted session, in call order. */
    val submittedSessionResults: MutableList<SessionResult> = mutableListOf()

    /**
     * [yield]s once before reporting anything back, mirroring
     * [com.rossomak.flashcards.core.data.repository.DefaultSessionSubmissionRepository]'s genuine
     * `withContext(Dispatchers.IO)` dispatcher hop.
     */
    override suspend fun submitSession(sessionResult: SessionResult): Result<Unit> {
        submittedSessionResults.add(sessionResult)
        yield()
        return resultToReturn
    }
}
