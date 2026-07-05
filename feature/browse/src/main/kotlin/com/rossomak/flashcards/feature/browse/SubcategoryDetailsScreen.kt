package com.rossomak.flashcards.feature.browse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.ui.composables.withInlineCode
import com.rossomak.flashcards.core.ui.navigation.observeAsEvents

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

@Composable
private fun FlashcardList(
    flashcards: List<Flashcard>,
    modifier: Modifier = Modifier,
) {
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(flashcards, key = { _, card -> card.id }) { index, flashcard ->
            FlashcardItem(
                index = index + 1,
                flashcard = flashcard,
                isExpanded = expandedStates[flashcard.id] ?: false,
                onToggleExpanded = { expandedStates[flashcard.id] = !(expandedStates[flashcard.id] ?: false) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun FlashcardItem(
    index: Int,
    flashcard: Flashcard,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.padding(end = 12.dp),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = index.toString(),
                                modifier = Modifier.padding(6.dp, 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Text(
                            text = flashcard.question.withInlineCode(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    if (flashcard.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .padding(start = 38.dp)
                        ) {
                            flashcard.tags.forEach { tag ->
                                Surface(
                                    modifier = Modifier.padding(end = 6.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                ) {
                                    Text(
                                        text = tag,
                                        modifier = Modifier.padding(6.dp, 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = flashcard.answer.withInlineCode(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 38.dp)
                    )
                }
            }
        }
    }
}
