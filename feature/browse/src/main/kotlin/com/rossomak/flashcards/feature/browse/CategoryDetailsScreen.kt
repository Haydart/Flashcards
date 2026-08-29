package com.rossomak.flashcards.feature.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AppBarRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.domain.model.Subcategory
import com.rossomak.flashcards.core.ui.composables.FlashcardsOverlineLabel
import com.rossomak.flashcards.core.ui.composables.bars.FlashcardsBottomToolbar
import com.rossomak.flashcards.core.ui.composables.bars.FlashcardsTopAppBar
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsFilledButton
import com.rossomak.flashcards.core.ui.composables.common.FlashcardsComponentSize
import com.rossomak.flashcards.core.ui.composables.flashcardsListScrollFade
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
    onNavigateToPreviewStudySession: (categoryId: String, categoryName: String, subcategoryIds: List<String>, subcategoryNames: List<String>) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CategoryDetailsContent(
        modifier = modifier,
        state = state,
        onNavigateBack = onNavigateBack,
        onNavigateToSubcategoryDetails = onNavigateToSubcategoryDetails,
        onStartSession = {
            onNavigateToPreviewStudySession(
                state.categoryId,
                state.categoryName,
                state.subcategories.map { subcategory -> subcategory.id },
                state.subcategories.map { subcategory -> subcategory.name },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsContent(
    modifier: Modifier = Modifier,
    state: CategoryDetailsScreenState,
    onNavigateBack: () -> Unit,
    onNavigateToSubcategoryDetails: (String, String, String, String) -> Unit,
    onStartSession: () -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                FlashcardsTopAppBar(
                    title = state.categoryName,
                    subtitle = stringResource(R.string.category_details_subtitle_label),
                    onNavigateBack = onNavigateBack,
                    scrollBehavior = scrollBehavior,
                    actions = { CategoryDetailsActions() },
                )
                if (!state.isLoading) {
                    FlashcardsOverlineLabel(
                        text = pluralStringResource(
                            R.plurals.browse_category_topic_count,
                            state.subcategories.size,
                            state.subcategories.size,
                        ),
                    )
                }
            }
        },
        bottomBar = {
            FlashcardsBottomToolbar(
                actions = { CategoryDetailsToolbarActions() },
                trailing = {
                    FlashcardsFilledButton(
                        text = stringResource(R.string.category_details_start_session_button),
                        onClick = onStartSession,
                        size = FlashcardsComponentSize.Small,
                        enabled = state.subcategories.isNotEmpty(),
                        icon = Icons.Filled.PlayArrow,
                    )
                },
            )
        },
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
                modifier = Modifier.padding(innerPadding),
                subcategories = state.subcategories,
                onNavigateToSubcategoryDetails = onNavigateToSubcategoryDetails,
            )
        }
    }
}

/**
 * Bookmark stays in the bar and everything past it falls into the overflow menu — [AppBarRow]
 * renders `maxItemCount - 1` items inline. Neither action is wired to the ViewModel yet: the
 * screen shows the action set the design calls for while the state behind it is still being built.
 */
@Composable
private fun RowScope.CategoryDetailsActions() {
    val bookmarkLabel = stringResource(R.string.category_details_bookmark_label)
    val addShortcutLabel = stringResource(R.string.category_details_add_shortcut_label)

    AppBarRow(
        overflowIndicator = { menuState ->
            IconButton(onClick = { menuState.show() }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.category_details_more_options_cd),
                )
            }
        },
        maxItemCount = 2,
    ) {
        clickableItem(
            onClick = {},
            icon = { Icon(imageVector = Icons.Filled.BookmarkBorder, contentDescription = null) },
            label = bookmarkLabel,
        )
        clickableItem(
            onClick = {},
            icon = { Icon(imageVector = Icons.Filled.AddToHomeScreen, contentDescription = null) },
            label = addShortcutLabel,
        )
    }
}

/** Filter, sort and add-topic, matching the Subcategory details toolbar. Not yet wired up. */
@Composable
private fun RowScope.CategoryDetailsToolbarActions() {
    IconButton(onClick = {}) {
        Icon(
            imageVector = Icons.Filled.FilterList,
            contentDescription = stringResource(R.string.category_details_filter_cd),
        )
    }
    IconButton(onClick = {}) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Sort,
            contentDescription = stringResource(R.string.category_details_sort_cd),
        )
    }
    IconButton(onClick = {}) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.category_details_add_topic_cd),
        )
    }
}

@Composable
private fun SubcategoryList(
    modifier: Modifier = Modifier,
    subcategories: List<Subcategory>,
    onNavigateToSubcategoryDetails: (String, String, String, String) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.normal)
            .flashcardsListGroupContainer(listState)
            .flashcardsListScrollFade(listState)
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
