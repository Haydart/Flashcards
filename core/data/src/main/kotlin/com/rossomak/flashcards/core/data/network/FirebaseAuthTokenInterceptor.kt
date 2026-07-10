package com.rossomak.flashcards.core.data.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the caller's Firebase ID token to every Cloud Function request; the function
 * verifies it server-side before touching ElevenLabs or the grading LLM. Runs on OkHttp's
 * worker thread, so the blocking token fetch is safe here.
 */
class FirebaseAuthTokenInterceptor @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val currentUser = firebaseAuth.currentUser ?: return chain.proceed(chain.request())
        val idToken = runCatching {
            Tasks.await(currentUser.getIdToken(false)).token
        }.getOrNull() ?: return chain.proceed(chain.request())
        val authenticatedRequest = chain.request().newBuilder()
            .header(AUTHORIZATION_HEADER, "Bearer $idToken")
            .build()
        return chain.proceed(authenticatedRequest)
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}
