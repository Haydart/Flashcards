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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.ui.composables.FlashcardsOverlineLabel
import com.rossomak.flashcards.core.ui.composables.bars.FlashcardsBottomToolbar
import com.rossomak.flashcards.core.ui.composables.bars.FlashcardsTopAppBar
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsFilledButton
import com.rossomak.flashcards.core.ui.composables.common.FlashcardsComponentSize
import com.rossomak.flashcards.core.ui.composables.flashcardsListScrollFade
import com.rossomak.flashcards.core.ui.composables.lists.FlashcardsListGroupItem
import com.rossomak.flashcards.core.ui.composables.lists.flashcardsListGroupContainer
import com.rossomak.flashcards.core.ui.composables.lists.flashcardsListGroupItems
import com.rossomak.flashcards.core.ui.composables.withInlineCode
import com.rossomak.flashcards.core.ui.navigation.observeAsEvents
import com.rossomak.flashcards.core.ui.theme.spacing
import kotlinx.collections.immutable.toImmutableList

@Composable
fun SubcategoryDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: SubcategoryDetailsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPreviewStudySession: (categoryId: String, categoryName: String, subcategoryId: String, subcategoryName: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    observeAsEvents(viewModel.events) { destination ->
        when (destination) {
            is SubcategoryDetailsDestination.PreviewStudySession ->
                onNavigateToPreviewStudySession(
                    destination.categoryId,
                    destination.categoryName,
                    destination.subcategoryId,
                    destination.subcategoryName,
                )
        }
    }

    SubcategoryDetailsContent(
        modifier = modifier,
        state = state,
        onNavigateBack = onNavigateBack,
        onStartSession = viewModel::onStartSession,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubcategoryDetailsContent(
    modifier: Modifier = Modifier,
    state: SubcategoryDetailsScreenState,
    onNavigateBack: () -> Unit,
    onStartSession: () -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                FlashcardsTopAppBar(
                    title = state.subcategoryName,
                    subtitle = stringResource(R.string.subcategory_details_subtitle_label, state.categoryName),
                    onNavigateBack = onNavigateBack,
                    scrollBehavior = scrollBehavior,
                    actions = { SubcategoryDetailsActions() },
                )
                if (!state.isLoading) {
                    FlashcardsOverlineLabel(
                        text = pluralStringResource(
                            R.plurals.subcategory_details_card_count_label,
                            state.flashcards.size,
                            state.flashcards.size,
                        ),
                    )
                }
            }
        },
        bottomBar = {
            FlashcardsBottomToolbar(
                actions = { SubcategoryDetailsToolbarActions() },
                trailing = {
                    FlashcardsFilledButton(
                        text = stringResource(R.string.subcategory_details_start_session_button),
                        onClick = onStartSession,
                        size = FlashcardsComponentSize.Small,
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
            else -> FlashcardList(
                modifier = Modifier.padding(innerPadding),
                flashcards = state.flashcards,
            )
        }
    }
}

/**
 * Bookmark stays in the bar; anything past it falls into the overflow menu, which is how
 * [AppBarRow] renders `maxItemCount - 1` items inline. None of these are wired to the ViewModel
 * yet — the screen shows the full action set the design calls for while the state that backs it is
 * still being built.
 */
@Composable
private fun RowScope.SubcategoryDetailsActions() {
    val bookmarkLabel = stringResource(R.string.subcategory_details_bookmark_label)
    val addShortcutLabel = stringResource(R.string.subcategory_details_add_shortcut_label)

    AppBarRow(
        overflowIndicator = { menuState ->
            IconButton(onClick = { menuState.show() }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.subcategory_details_more_options_cd),
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

/** Filter, sort and add-card, in the order ADR-0022 fixes them. Not yet wired to the ViewModel. */
@Composable
private fun RowScope.SubcategoryDetailsToolbarActions() {
    IconButton(onClick = {}) {
        Icon(
            imageVector = Icons.Filled.FilterList,
            contentDescription = stringResource(R.string.subcategory_details_filter_cd),
        )
    }
    IconButton(onClick = {}) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Sort,
            contentDescription = stringResource(R.string.subcategory_details_sort_cd),
        )
    }
    IconButton(onClick = {}) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.subcategory_details_add_card_cd),
        )
    }
}

/**
 * A subcategory's flashcards can run into the dozens, so this binds `flashcardsListGroupItems`
 * inside a `LazyColumn` rather than the bounded `FlashcardsListGroup` — only rows near the
 * viewport get composed.
 */
@Composable
private fun FlashcardList(
    modifier: Modifier = Modifier,
    flashcards: List<Flashcard>,
) {
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    val expandedStateDescription = stringResource(R.string.subcategory_details_card_expanded_cd)
    val collapsedStateDescription = stringResource(R.string.subcategory_details_card_collapsed_cd)

    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.normal)
            .flashcardsListGroupContainer(listState)
            .flashcardsListScrollFade(listState),
    ) {
        flashcardsListGroupItems(
            items = flashcards.map { flashcard ->
                FlashcardsListGroupItem.ExpandableRow(
                    key = flashcard.id,
                    difficulty = flashcard.difficulty,
                    title = flashcard.question,
                    expanded = expandedStates[flashcard.id] ?: false,
                    onExpandedChange = { expanded -> expandedStates[flashcard.id] = expanded },
                    expandedStateDescription = expandedStateDescription,
                    collapsedStateDescription = collapsedStateDescription,
                    tags = flashcard.tags.toImmutableList(),
                    expandedContent = {
                        Text(
                            text = flashcard.answer.withInlineCode(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                )
            },
        )
    }
}
