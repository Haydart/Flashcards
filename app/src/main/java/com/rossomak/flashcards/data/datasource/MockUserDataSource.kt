package com.rossomak.flashcards.data.datasource

import com.rossomak.flashcards.domain.model.User
import kotlinx.coroutines.delay
import javax.inject.Inject

class MockUserDataSource @Inject constructor() {

    suspend fun getCurrentUser(): User {
        delay(2000L)
        return User(id = "1", name = "User One")
    }
}
