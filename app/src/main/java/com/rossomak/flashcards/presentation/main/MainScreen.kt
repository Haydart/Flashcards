package com.rossomak.flashcards.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rossomak.flashcards.ui.theme.FlashcardsTheme

private val AvatarSize = 120.dp

@Composable
fun MainRoute(
    onNavigateToLogin: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.signedOutEvents.collect { onNavigateToLogin() }
    }

    MainScreen(
        state = state,
        onSignOutClick = viewModel::onSignOutClick
    )
}

@Composable
fun MainScreen(
    state: MainScreenState,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isLoading -> CircularProgressIndicator()

            state.error != null -> Text(
                text = "Error: ${state.error}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )

            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Avatar(photoUrl = state.photoUrl)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Hello, ${state.displayName ?: "User"}!",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButton(onClick = onSignOutClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Sign out")
                }
            }
        }
    }
}

@Composable
private fun Avatar(photoUrl: String?) {
    if (photoUrl.isNullOrBlank()) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(AvatarSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Profile photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(AvatarSize)
                .clip(CircleShape)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenLoadingPreview() {
    FlashcardsTheme {
        MainScreen(state = MainScreenState(isLoading = true), onSignOutClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenSignedInWithPhotoPreview() {
    FlashcardsTheme {
        MainScreen(
            state = MainScreenState(
                isLoading = false,
                displayName = "John",
                photoUrl = "https://example.com/photo.jpg"
            ),
            onSignOutClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenSignedInNoPhotoPreview() {
    FlashcardsTheme {
        MainScreen(
            state = MainScreenState(
                isLoading = false,
                displayName = "John",
                photoUrl = null
            ),
            onSignOutClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenErrorPreview() {
    FlashcardsTheme {
        MainScreen(
            state = MainScreenState(isLoading = false, error = "Something went wrong"),
            onSignOutClick = {}
        )
    }
}
