package com.rossomak.flashcards.data.repository

import com.rossomak.flashcards.data.datasource.MockUserDataSource
import com.rossomak.flashcards.domain.model.User
import com.rossomak.flashcards.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val dataSource: MockUserDataSource
) : UserRepository {
    override suspend fun getCurrentUser(): Result<User> {
        return try {
            Result.success(dataSource.getCurrentUser())
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}