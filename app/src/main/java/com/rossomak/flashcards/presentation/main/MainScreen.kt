package com.rossomak.flashcards.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.ui.theme.FlashcardsTheme

@Composable
fun MainRoute(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MainScreen(state = state)
}

@Composable
fun MainScreen(state: MainScreenState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator()
            }
            state.error != null -> {
                Text(
                    text = "Error: ${state.error}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
            state.message != null -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenLoadingPreview() {
    FlashcardsTheme {
        MainScreen(state = MainScreenState(isLoading = true))
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenMessagePreview() {
    FlashcardsTheme {
        MainScreen(state = MainScreenState(isLoading = false, message = "Hello, world"))
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenErrorPreview() {
    FlashcardsTheme {
        MainScreen(state = MainScreenState(isLoading = false, error = "Something went wrong"))
    }
}
