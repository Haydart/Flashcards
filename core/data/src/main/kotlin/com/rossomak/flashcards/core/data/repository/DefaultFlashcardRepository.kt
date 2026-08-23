package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.mapper.toDomain
import com.rossomak.flashcards.core.data.source.FlashcardRemoteDataSource
import com.rossomak.flashcards.core.domain.model.Category
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.Subcategory
import com.rossomak.flashcards.core.domain.repository.FlashcardRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultFlashcardRepository @Inject constructor(
    private val remoteDataSource: FlashcardRemoteDataSource
) : FlashcardRepository {

    override suspend fun fetchCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            Result.success(remoteDataSource.getCategories().map { it.toDomain() })
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun fetchSubcategories(categoryId: String): Result<List<Subcategory>> = withContext(Dispatchers.IO) {
        try {
            Result.success(
                remoteDataSource.getSubcategoriesByCategoryId(categoryId).map { it.toDomain() }
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun searchSubcategories(namePrefix: String): Result<List<Subcategory>> = withContext(Dispatchers.IO) {
        try {
            Result.success(
                remoteDataSource.searchSubcategoriesByNamePrefix(namePrefix).map { it.toDomain() }
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun fetchFlashcards(subcategoryId: String): Result<List<Flashcard>> = withContext(Dispatchers.IO) {
        try {
            Result.success(
                remoteDataSource.getFlashcardsBySubcategoryId(subcategoryId)
                    .mapNotNull { it.toDomain(subcategoryId) }
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
