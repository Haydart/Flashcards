package com.rossomak.flashcards.feature.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.domain.model.Category
import com.rossomak.flashcards.core.ui.composables.FlashcardsIconTile
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
            else -> CategoryList(categories = state.categories, onCategoryClick = onCategoryClick)
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
    categories: List<Category>,
    onCategoryClick: (String, String) -> Unit,
) {
    if (categories.isEmpty()) {
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
            items = categories.mapIndexed { index, category ->
                category.toListGroupItem(
                    index = index,
                    subcategoryCountText = pluralStringResource(
                        R.plurals.browse_category_subcategory_count,
                        category.subcategoryCount,
                        category.subcategoryCount,
                    ),
                    placeholderSubtitle = stringResource(R.string.browse_category_placeholder_subtitle),
                    onCategoryClick = onCategoryClick,
                )
            },
        )
    }
}

/**
 * Placeholder icon/color per category row, cycled by position — standing in for
 * `Category.iconUrl` (not yet wired end-to-end; see docs/design/category-icon-color.md). Not
 * keyed by category name/content: only [Category.name] and [Category.subcategoryCount] are real
 * backend fields, so nothing here should look tied to a specific category's identity. The
 * [FlashcardsListGroupItem.DetailedRow] subtitle line is the same static
 * `R.string.browse_category_placeholder_subtitle` copy on every row for the same reason — there
 * is no backend topic-summary field yet to show instead.
 */
private val categoryIconPalette = listOf(
    Icons.Default.School to Color(0xFF6B2FA0),
    Icons.Default.MenuBook to Color(0xFF0277BD),
    Icons.Default.Extension to Color(0xFF00838F),
    Icons.Default.AutoAwesome to Color(0xFFAD1457),
    Icons.Default.Bolt to Color(0xFF558B2F),
    Icons.Default.Star to Color(0xFFD84315),
)

private const val CATEGORY_TILE_ALPHA = 0.12f

private fun Category.toListGroupItem(
    index: Int,
    subcategoryCountText: String,
    placeholderSubtitle: String,
    onCategoryClick: (String, String) -> Unit,
): FlashcardsListGroupItem {
    val (icon, color) = categoryIconPalette[index % categoryIconPalette.size]
    return FlashcardsListGroupItem.DetailedRow(
        key = id,
        title = name,
        subtitle = placeholderSubtitle,
        secondaryText = subcategoryCountText,
        onClick = { onCategoryClick(id, name) },
        leading = {
            FlashcardsIconTile(
                icon = icon,
                contentDescription = null,
                containerColor = color.copy(alpha = CATEGORY_TILE_ALPHA),
                contentColor = color,
            )
        },
        trailing = { FlashcardsChevron() },
    )
}
