package com.rossomak.flashcards.feature.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.domain.model.Category
import com.rossomak.flashcards.core.ui.composables.lists.FlashcardsChevron
import com.rossomak.flashcards.core.ui.composables.lists.FlashcardsListGroup
import com.rossomak.flashcards.core.ui.composables.lists.FlashcardsListGroupItem
import com.rossomak.flashcards.core.ui.navigation.observeAsEvents
import com.rossomak.flashcards.core.ui.theme.spacing

@Composable
fun BrowseScreen(
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = hiltViewModel(),
    onNavigateToCategoryDetails: (String, String) -> Unit = { _, _ -> },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    observeAsEvents(viewModel.events) { destination ->
        when (destination) {
            is BrowseNavigationDestination.CategoryDetails ->
                onNavigateToCategoryDetails(destination.categoryId, destination.categoryName)
        }
    }

    BrowseContent(
        modifier = modifier,
        state = state,
        onRefresh = viewModel::onCategoriesRefresh,
        onCategoryClick = viewModel::onCategorySelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseContent(
    modifier: Modifier = Modifier,
    state: BrowseScreenState,
    onRefresh: () -> Unit,
    onCategoryClick: (String, String) -> Unit,
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

/**
 * Categories are a short, fixed set (roughly a dozen) so this binds [FlashcardsListGroup]
 * directly rather than a `LazyColumn` — every row composes up front at negligible cost. A
 * subcategory list nested under one category can run into the dozens and should use
 *
 * `flashcardsListGroupItems` inside a `LazyColumn` instead.
 */
@Composable
private fun CategoryList(
    state: BrowseScreenState,
    onCategoryClick: (String, String) -> Unit,
) {
    if (state.categories.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No categories found")
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.normal),
    ) {
        FlashcardsListGroup(
            items = state.categories.map { category -> category.toListGroupItem(onCategoryClick) },
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.normal))
        MockSelectableList()
    }
}

/** Mock rows to eyeball [FlashcardsSelectableListRow]'s coloring/background after the rework. */
@Composable
private fun MockSelectableList() {
    val mockTitles = listOf("Compose", "Coroutines", "Testing", "Networking", "Dependency Injection")
    val selectedStates = remember { mutableStateMapOf<String, Boolean>() }

    FlashcardsListGroup(
        items = mockTitles.mapIndexed { index, title ->
            FlashcardsListGroupItem.Selectable(
                key = title,
                title = title,
                selected = selectedStates[title] ?: (index == 0),
                onSelectedChange = { selected -> selectedStates[title] = selected },
                subtitle = "${(index + 1) * 10} cards",
                trailing = { FlashcardsChevron() },
            )
        },
    )
}

private fun Category.toListGroupItem(onCategoryClick: (String, String) -> Unit) =
    FlashcardsListGroupItem.Row(
        key = id,
        title = name,
        onClick = { onCategoryClick(id, name) },
        subtitle = "$subcategoryCount subcategories",
        trailing = { FlashcardsChevron() },
    )
