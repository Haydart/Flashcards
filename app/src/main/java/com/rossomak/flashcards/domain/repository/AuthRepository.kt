package com.rossomak.flashcards.domain.repository

import com.rossomak.flashcards.domain.model.AuthUser

interface AuthRepository {
    fun getCurrentUser(): AuthUser?
    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser>
    fun signOut()
}
