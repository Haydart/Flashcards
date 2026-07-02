package com.rossomak.flashcards.feature.study.prestart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun StudyScreen(
    onNavigateToCategoryDetails: (String, String) -> Unit = { _, _ -> },
    viewModel: StudyViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigationDestination) {
        when (val destination = state.navigationDestination) {
            is StudyNavigationDestination.CategoryDetails -> {
                onNavigateToCategoryDetails(destination.categoryId, destination.categoryName)
                viewModel.onNavigationHandled()
            }
            null -> Unit
        }
    }

    StudyContent(
        state = state,
        onRefresh = viewModel::onCategoriesRefresh,
        onCategoryClick = viewModel::onCategorySelected,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyContent(
    state: StudyScreenState,
    onRefresh: () -> Unit,
    onCategoryClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            state.error != null -> Text(
                text = "Error: ${state.error}",
                modifier = Modifier.align(Alignment.Center)
            )
            else -> CategoryList(state = state, onCategoryClick = onCategoryClick)
        }
    }
}

@Composable
private fun CategoryList(
    state: StudyScreenState,
    onCategoryClick: (String, String) -> Unit,
) {
    if (state.categories.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No categories found")
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.categories, key = { it.id }) { category ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategoryClick(category.id, category.name) }
                    .padding(16.dp)
            ) {
                Text(text = category.name)
                Text(text = "${category.subcategoryCount} subcategories")
            }
        }
    }
}
