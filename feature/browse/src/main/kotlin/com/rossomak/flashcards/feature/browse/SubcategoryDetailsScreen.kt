package com.rossomak.flashcards.feature.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.ui.composables.lists.FlashcardsListGroupItem
import com.rossomak.flashcards.core.ui.composables.lists.flashcardsListGroupContainer
import com.rossomak.flashcards.core.ui.composables.lists.flashcardsListGroupItems
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
            LargeTopAppBar(
                title = { Text(text = state.subcategoryName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
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
            else -> Column(modifier = Modifier.padding(innerPadding)) {
                Button(
                    onClick = onStartSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("Start Session")
                }
                FlashcardList(
                    flashcards = state.flashcards,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * A subcategory's flashcards can run into the dozens, so this binds `flashcardsListGroupItems`
 * inside a `LazyColumn` rather than the bounded `FlashcardsListGroup` — only rows near the
 * viewport get composed.
 */
@Composable
private fun FlashcardList(
    flashcards: List<Flashcard>,
    modifier: Modifier = Modifier,
) {
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    val expandedStateDescription = stringResource(R.string.subcategory_details_card_expanded_cd)
    val collapsedStateDescription = stringResource(R.string.subcategory_details_card_collapsed_cd)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.normal)
            .flashcardsListGroupContainer(),
    ) {
        flashcardsListGroupItems(
            items = flashcards.map { flashcard ->
                FlashcardsListGroupItem.ExpandableCard(
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
                            text = flashcard.answer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                )
            },
        )
    }
}
