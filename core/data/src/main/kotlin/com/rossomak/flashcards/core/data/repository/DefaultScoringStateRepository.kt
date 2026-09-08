package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.mapper.toDomain
import com.rossomak.flashcards.core.data.source.ScoringStateRemoteDataSource
import com.rossomak.flashcards.core.domain.model.ScoringState
import com.rossomak.flashcards.core.domain.repository.ScoringStateRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultScoringStateRepository @Inject constructor(
    private val remoteDataSource: ScoringStateRemoteDataSource,
) : ScoringStateRepository {

    override suspend fun getScoringState(): Result<ScoringState?> = withContext(Dispatchers.IO) {
        try {
            Result.success(remoteDataSource.getScoringState()?.toDomain())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
