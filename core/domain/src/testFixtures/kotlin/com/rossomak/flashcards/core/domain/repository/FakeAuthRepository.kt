package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.AuthUser

class FakeAuthRepository : AuthRepository {
    var userToReturn: AuthUser? = null
    var signInResult: Result<AuthUser> = Result.failure(UnsupportedOperationException("not configured"))

    override fun getCurrentUser(): AuthUser? = userToReturn

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser> =
        signInResult.onSuccess { userToReturn = it }

    override fun signOut() {
        userToReturn = null
    }
}
