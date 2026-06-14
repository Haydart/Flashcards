package com.rossomak.flashcards.presentation.subcategorydetails

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SubcategoryDetailsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubcategoryDetailsViewModel = hiltViewModel(),
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Subcategory Details Screen, NYI")
    }
}
