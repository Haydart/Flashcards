package com.rossomak.flashcards.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.rossomak.flashcards.core.domain.model.AuthUser
import com.rossomak.flashcards.core.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DefaultAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override fun getCurrentUser(): AuthUser? = firebaseAuth.currentUser?.toAuthUser()

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Firebase user was null after sign-in"))
            Result.success(user.toAuthUser())
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString()
    )
}
