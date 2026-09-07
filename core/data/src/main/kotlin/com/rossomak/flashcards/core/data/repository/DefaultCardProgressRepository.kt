package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.mapper.toDomain
import com.rossomak.flashcards.core.data.source.CardProgressRemoteDataSource
import com.rossomak.flashcards.core.data.source.ProgressSummaryRemoteDataSource
import com.rossomak.flashcards.core.domain.model.ProgressSummary
import com.rossomak.flashcards.core.domain.model.SubcategoryProgress
import com.rossomak.flashcards.core.domain.repository.CardProgressRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultCardProgressRepository @Inject constructor(
    private val remoteDataSource: CardProgressRemoteDataSource,
    private val progressSummaryRemoteDataSource: ProgressSummaryRemoteDataSource,
) : CardProgressRepository {

    override suspend fun getProgress(subcategoryId: String): Result<SubcategoryProgress?> = withContext(Dispatchers.IO) {
        try {
            Result.success(remoteDataSource.getProgress(subcategoryId)?.toDomain(subcategoryId))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun getProgressSummary(): Result<ProgressSummary?> = withContext(Dispatchers.IO) {
        try {
            Result.success(progressSummaryRemoteDataSource.getSummary()?.toDomain())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
