package com.rossomak.flashcards.feature.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.domain.model.Subcategory

@Composable
fun CategoryDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: CategoryDetailsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSubcategoryDetails: (String, String, String, String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CategoryDetailsContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onNavigateToSubcategoryDetails = onNavigateToSubcategoryDetails,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsContent(
    state: CategoryDetailsScreenState,
    onNavigateBack: () -> Unit,
    onNavigateToSubcategoryDetails: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier,
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
                categoryId = state.categoryId,
                categoryName = state.categoryName,
                onNavigateToSubcategoryDetails = onNavigateToSubcategoryDetails,
                subcategories = state.subcategories,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun SubcategoryList(
    categoryId: String,
    categoryName: String,
    onNavigateToSubcategoryDetails: (String, String, String, String) -> Unit,
    subcategories: List<Subcategory>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(subcategories, key = { it.id }) { subcategory ->
            SubcategoryRow(
                categoryId = categoryId,
                categoryName = categoryName,
                subcategory = subcategory,
                onNavigateToSubcategoryDetails = onNavigateToSubcategoryDetails
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun SubcategoryRow(
    categoryId: String,
    categoryName: String,
    subcategory: Subcategory,
    onNavigateToSubcategoryDetails: (String, String, String, String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNavigateToSubcategoryDetails(
                    categoryId,
                    categoryName,
                    subcategory.id,
                    subcategory.name
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subcategory.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${subcategory.cardCount} cards",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
