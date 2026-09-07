package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.source.StudySessionRemoteDataSource
import com.rossomak.flashcards.core.domain.model.SessionCommit
import com.rossomak.flashcards.core.domain.repository.StudySessionRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultStudySessionRepository @Inject constructor(
    private val remoteDataSource: StudySessionRemoteDataSource,
) : StudySessionRepository {

    override suspend fun commitSession(sessionCommit: SessionCommit, onRejected: (Throwable) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                remoteDataSource.commitSession(sessionCommit, onRejected)
                Result.success(Unit)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
}
