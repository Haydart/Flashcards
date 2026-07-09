package com.rossomak.flashcards.core.data.source

import com.rossomak.flashcards.core.domain.model.AuthUser

interface AuthRemoteDataSource {

    fun getCurrentUser(): AuthUser?

    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser>

    fun signOut()
}
