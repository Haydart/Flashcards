package com.rossomak.flashcards.feature.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.domain.model.Subcategory
import com.rossomak.flashcards.core.ui.composables.lists.FlashcardsListGroupItem
import com.rossomak.flashcards.core.ui.composables.lists.flashcardsListGroupContainer
import com.rossomak.flashcards.core.ui.composables.lists.flashcardsListGroupItems
import com.rossomak.flashcards.core.ui.theme.spacing

@Composable
fun CategoryDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: CategoryDetailsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSubcategoryDetails: (String, String, String, String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CategoryDetailsContent(
        modifier = modifier,
        state = state,
        onNavigateBack = onNavigateBack,
        onNavigateToSubcategoryDetails = onNavigateToSubcategoryDetails,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsContent(
    modifier: Modifier = Modifier,
    state: CategoryDetailsScreenState,
    onNavigateBack: () -> Unit,
    onNavigateToSubcategoryDetails: (String, String, String, String) -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = state.categoryName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = state.error)
            }

            else -> SubcategoryList(
                subcategories = state.subcategories,
                modifier = Modifier.padding(innerPadding),
                onNavigateToSubcategoryDetails = onNavigateToSubcategoryDetails,
            )
        }
    }
}

@Composable
private fun SubcategoryList(
    modifier: Modifier = Modifier,
    subcategories: List<Subcategory>,
    onNavigateToSubcategoryDetails: (String, String, String, String) -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.normal)
            .flashcardsListGroupContainer()
    ) {
        flashcardsListGroupItems(
            items = subcategories.map { subcategory ->
                FlashcardsListGroupItem.Row(
                    key = subcategory.id,
                    title = subcategory.name,
                    secondaryText = "${subcategory.cardCount} cards",
                    onClick = { onNavigateToSubcategoryDetails(subcategory.categoryId, subcategory.categoryName, subcategory.id, subcategory.name) }
                )
            }
        )
    }
}
