package com.rossomak.flashcards.domain.repository

import com.rossomak.flashcards.domain.model.User

interface UserRepository {
    suspend fun getCurrentUser(): Result<User>
}
