package com.rossomak.flashcards.presentation.studysession

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallatinapps.syntaxmp.tokenizer.SyntaxTokenizer
import com.rossomak.flashcards.ui.text.SyntaxCodeBlock
import com.rossomak.flashcards.ui.text.withInlineCode

@Composable
fun StudySessionScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudySessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSessionComplete) {
        if (state.isSessionComplete) onNavigateBack()
    }

    StudySessionContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onShowAnswer = viewModel::onShowAnswer,
        onNextCard = viewModel::onNextCard,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionContent(
    state: StudySessionScreenState,
    onNavigateBack: () -> Unit,
    onShowAnswer: () -> Unit,
    onNextCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = state.subcategoryName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit session",
                        )
                    }
                },
                actions = {
                    if (state.flashcards.isNotEmpty()) {
                        Text(
                            text = "${state.currentCardIndex + 1} / ${state.flashcards.size}",
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
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
            state.flashcards.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "No cards available")
            }
            else -> {
                val card = state.flashcards[state.currentCardIndex]
                val isLastCard = state.currentCardIndex == state.flashcards.lastIndex
                var isExtendedContextExpanded by remember(card.id) { mutableStateOf(false) }
                val syntaxEngine = remember { SyntaxTokenizer() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Question",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "Difficulty ${card.difficulty}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = card.question.withInlineCode(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        card.questionCode?.forEach { codeBlock ->
                            Spacer(modifier = Modifier.height(8.dp))
                            SyntaxCodeBlock(
                                code = codeBlock.code,
                                language = codeBlock.language,
                                engine = syntaxEngine,
                            )
                        }
                        AnimatedVisibility(
                            visible = state.isAnswerRevealed,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Answer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = card.answer.withInlineCode(),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                card.answerCode?.forEach { codeBlock ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SyntaxCodeBlock(
                                        code = codeBlock.code,
                                        language = codeBlock.language,
                                        engine = syntaxEngine,
                                    )
                                }
                                if (!card.extendedContext.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isExtendedContextExpanded = !isExtendedContextExpanded }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Extended context",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Icon(
                                            imageVector = if (isExtendedContextExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExtendedContextExpanded) "Collapse" else "Expand",
                                        )
                                    }
                                    AnimatedVisibility(
                                        visible = isExtendedContextExpanded,
                                        enter = expandVertically(),
                                        exit = shrinkVertically(),
                                    ) {
                                        Text(
                                            text = card.extendedContext.withInlineCode(),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(bottom = 12.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (!state.isAnswerRevealed) {
                            Button(
                                onClick = onShowAnswer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Show Answer")
                            }
                        } else {
                            OutlinedButton(
                                onClick = onNextCard,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (isLastCard) "Finish" else "Next")
                            }
                        }
                    }
                }
            }
        }
    }
}
