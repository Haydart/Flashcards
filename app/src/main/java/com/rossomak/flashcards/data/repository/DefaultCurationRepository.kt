package com.rossomak.flashcards.data.repository

import com.rossomak.flashcards.data.mapper.toDomain
import com.rossomak.flashcards.data.source.CurationRemoteDataSource
import com.rossomak.flashcards.domain.model.CurationAction
import com.rossomak.flashcards.domain.model.CurationRequest
import com.rossomak.flashcards.domain.repository.CurationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultCurationRepository @Inject constructor(
    private val remoteDataSource: CurationRemoteDataSource,
) : CurationRepository {

    override suspend fun getCurationRequests(cardIds: List<String>): Result<Map<String, CurationRequest>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(
                    remoteDataSource.getCurationRequests(cardIds)
                        .mapValues { (cardId, dto) -> dto.toDomain(cardId) }
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }

    override suspend fun upsertCurationAction(
        cardId: String,
        subcategoryId: String,
        action: CurationAction,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Result.success(remoteDataSource.upsertCurationAction(cardId, subcategoryId, action))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun removeCurationAction(cardId: String, action: CurationAction): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(remoteDataSource.removeCurationAction(cardId, action))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
}
