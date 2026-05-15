package com.rossomak.flashcards.presentation.login

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.rossomak.flashcards.BuildConfig
import com.rossomak.flashcards.R
import kotlinx.coroutines.launch

private const val TAG = "LoginScreen"

private val LoginGradientStart = Color(0xFF7B2FBE)
private val LoginGradientEnd = Color(0xFF2979FF)

private val loginGradient = Brush.linearGradient(
    colors = listOf(LoginGradientStart, LoginGradientEnd),
    start = Offset.Zero,
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

private val LogoWidth = 200.dp

@Composable
fun LoginRoute(
    onNavigateToMain: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { onNavigateToMain() }
    }

    LoginScreen(
        state = state,
        onGoogleSignInClick = {
            if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
                viewModel.onSignInFailed("Missing GOOGLE_WEB_CLIENT_ID. Add it to local.properties.")
            } else {
                viewModel.onSignInStarted()
                coroutineScope.launch {
                    try {
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
                        val googleCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
                        viewModel.onGoogleIdTokenReceived(googleCredential.idToken)
                    } catch (exception: GetCredentialException) {
                        Log.e(TAG, "Credential Manager failed", exception)
                        val message = when (exception) {
                            is NoCredentialException ->
                                "No Google account on this device. Add one in Settings → Accounts."
                            is GetCredentialCancellationException ->
                                "Sign-in cancelled."
                            else -> exception.localizedMessage
                        }
                        viewModel.onSignInFailed(message)
                    } catch (exception: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Failed to parse Google ID token", exception)
                        viewModel.onSignInFailed(exception.localizedMessage)
                    }
                }
            }
        }
    )
}

@Composable
fun LoginScreen(
    state: LoginScreenState,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(loginGradient)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(R.drawable.flashcards_white),
                contentDescription = null,
                modifier = Modifier.width(LogoWidth)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onGoogleSignInClick,
                enabled = !state.isSigningIn,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1F1F1F)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                if (state.isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF1F1F1F)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Signing in…", fontWeight = FontWeight.Medium)
                } else {
                    Text(text = "Sign in with Google", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                }
            }

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = state.errorMessage,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun LoginScreenPreview() {
    LoginScreen(state = LoginScreenState(), onGoogleSignInClick = {})
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun LoginScreenSigningInPreview() {
    LoginScreen(state = LoginScreenState(isSigningIn = true), onGoogleSignInClick = {})
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun LoginScreenErrorPreview() {
    LoginScreen(
        state = LoginScreenState(errorMessage = "Sign-in cancelled"),
        onGoogleSignInClick = {}
    )
}
