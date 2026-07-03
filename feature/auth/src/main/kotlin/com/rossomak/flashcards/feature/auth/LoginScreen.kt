package com.rossomak.flashcards.feature.auth

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.ui.navigation.observeAsEvents
import kotlinx.coroutines.launch

private val LoginGradientStart = Color(0xFF7B2FBE)
private val LoginGradientEnd = Color(0xFF2979FF)

private val loginGradient = Brush.linearGradient(
    colors = listOf(LoginGradientStart, LoginGradientEnd),
    start = Offset.Zero,
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

private val LogoWidth = 200.dp

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToMain: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val signInLauncher = remember(context) { GoogleSignInLauncher(context) }

    observeAsEvents(viewModel.events) { destination ->
        when (destination) {
            LoginDestination.Main -> onNavigateToMain()
        }
    }

    LoginContent(
        modifier = modifier,
        state = state,
        onGoogleSignInClick = {
            viewModel.onSignInStarted()
            coroutineScope.launch {
                signInLauncher.launch()
                    .onSuccess { idToken -> viewModel.onGoogleIdTokenReceived(idToken) }
                    .onFailure { error -> viewModel.onSignInFailed(error.message) }
            }
        },
    )
}

@Composable
fun LoginContent(
    modifier: Modifier = Modifier,
    state: LoginScreenState,
    onGoogleSignInClick: () -> Unit,
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
private fun LoginContentPreview() {
    LoginContent(state = LoginScreenState(), onGoogleSignInClick = {})
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun LoginContentSigningInPreview() {
    LoginContent(state = LoginScreenState(isSigningIn = true), onGoogleSignInClick = {})
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun LoginContentErrorPreview() {
    LoginContent(
        state = LoginScreenState(errorMessage = "Sign-in cancelled"),
        onGoogleSignInClick = {}
    )
}
