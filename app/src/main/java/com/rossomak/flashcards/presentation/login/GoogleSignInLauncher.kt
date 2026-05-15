package com.rossomak.flashcards.presentation.login

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.rossomak.flashcards.BuildConfig

class GoogleSignInLauncher(private val context: Context) {

    suspend fun launch(): Result<String> {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            return Result.failure(IllegalStateException("Missing GOOGLE_WEB_CLIENT_ID. Add it to local.properties."))
        }
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val response = CredentialManager.create(context).getCredential(
                context = context,
                request = request
            )
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
            Result.success(credential.idToken)
        } catch (exception: GetCredentialException) {
            Log.e(TAG, "Credential Manager failed", exception)
            val message = when (exception) {
                is NoCredentialException -> "No Google account on this device. Add one in Settings → Accounts."
                is GetCredentialCancellationException -> "Sign-in cancelled."
                else -> exception.localizedMessage ?: "Sign-in failed"
            }
            Result.failure(Exception(message))
        } catch (exception: GoogleIdTokenParsingException) {
            Log.e(TAG, "Failed to parse Google ID token", exception)
            Result.failure(Exception(exception.localizedMessage ?: "Sign-in failed"))
        }
    }

    private companion object {
        private const val TAG = "GoogleSignInLauncher"
    }
}
