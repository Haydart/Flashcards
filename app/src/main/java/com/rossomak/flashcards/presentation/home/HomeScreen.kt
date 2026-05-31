package com.rossomak.flashcards.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeScreen(
    onNavigateToCategoryDetails: (String) -> Unit = {},
    onNavigateToSubcategoryDetails: (String, String) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigationDestination) {
        when (val destination = state.navigationDestination) {
            is HomeDestination.CategoryDetails -> onNavigateToCategoryDetails(destination.categoryId)
            is HomeDestination.SubcategoryDetails -> onNavigateToSubcategoryDetails(destination.categoryId, destination.subcategoryId)
            null -> Unit
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = state.title)
    }
}
