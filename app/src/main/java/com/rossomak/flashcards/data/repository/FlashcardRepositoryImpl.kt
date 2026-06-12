package com.rossomak.flashcards.data.repository

import com.rossomak.flashcards.data.mapper.toDomain
import com.rossomak.flashcards.data.source.FlashcardRemoteDataSource
import com.rossomak.flashcards.domain.model.Category
import com.rossomak.flashcards.domain.model.Flashcard
import com.rossomak.flashcards.domain.model.Subcategory
import com.rossomak.flashcards.domain.repository.FlashcardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FlashcardRepositoryImpl @Inject constructor(
    private val remoteDataSource: FlashcardRemoteDataSource
) : FlashcardRepository {

    override suspend fun fetchCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            Result.success(remoteDataSource.getCategories().map { it.toDomain() })
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun fetchSubcategories(categoryId: String): Result<List<Subcategory>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(
                    remoteDataSource.getSubcategoriesByCategoryId(categoryId).map { it.toDomain() }
                )
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }

    override suspend fun fetchFlashcards(subcategoryId: String): Result<List<Flashcard>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(
                    remoteDataSource.getFlashcardsBySubcategoryId(subcategoryId)
                        .map { it.toDomain(subcategoryId) }
                )
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
}
