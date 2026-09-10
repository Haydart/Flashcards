package com.rossomak.flashcards.feature.study.chrome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gallatinapps.syntaxmp.tokenizer.SyntaxTokenizer
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.ui.composables.SyntaxCodeBlock
import com.rossomak.flashcards.core.ui.composables.withInlineCode
import com.rossomak.flashcards.feature.study.R

/**
 * The non-sheet body shared by every Study Session screen
 * ([ADR-0045](../../../../../../../../docs/adr/0045-separate-fast-and-rated-session-screens.md)):
 * the loading spinner, the load-error text, the empty-deck message, and — once cards exist — the
 * card surface itself (tag row, question, and the animated reveal of the answer).
 *
 * Takes the current card plus the values needed to pick it, not the screen state that owns them —
 * the two Study Modes are diverging into two different state types and this body must not force
 * either into a shared supertype just to be fed.
 */
@Composable
fun StudySessionBody(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    error: String?,
    flashcards: List<Flashcard>,
    currentCardIndex: Int,
    isAnswerRevealed: Boolean,
    innerPadding: PaddingValues,
    onExtendedContextClick: (String) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> CenteredBox(innerPadding) { CircularProgressIndicator() }
            error != null -> CenteredBox(innerPadding) { Text(text = error) }
            flashcards.isEmpty() -> CenteredBox(innerPadding) {
                Text(text = stringResource(R.string.study_session_no_cards_message))
            }

            else -> StudySessionCard(
                card = flashcards[currentCardIndex],
                isAnswerRevealed = isAnswerRevealed,
                innerPadding = innerPadding,
                onExtendedContextClick = onExtendedContextClick,
            )
        }
    }
}

@Composable
private fun StudySessionCard(
    card: Flashcard,
    isAnswerRevealed: Boolean,
    innerPadding: PaddingValues,
    onExtendedContextClick: (String) -> Unit,
) {
    val syntaxEngine = remember { SyntaxTokenizer() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (card.tags.isNotEmpty()) {
            TagRow(tags = card.tags)
            Spacer(modifier = Modifier.height(12.dp))
        }
        QuestionSection(card = card, syntaxEngine = syntaxEngine)
        AnimatedVisibility(
            visible = isAnswerRevealed,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            AnswerSection(
                card = card,
                syntaxEngine = syntaxEngine,
                onExtendedContextClick = onExtendedContextClick,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagRow(tags: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(6.dp, 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun QuestionSection(card: Flashcard, syntaxEngine: SyntaxTokenizer) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.study_session_question_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.study_session_difficulty_label, card.difficulty),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = card.question.withInlineCode(),
        style = MaterialTheme.typography.bodyLarge
    )
    card.questionCode?.forEach { codeBlock ->
        Spacer(modifier = Modifier.height(8.dp))
        SyntaxCodeBlock(
            code = codeBlock.code,
            language = codeBlock.language,
            engine = syntaxEngine
        )
    }
}

@Composable
private fun AnswerSection(
    card: Flashcard,
    syntaxEngine: SyntaxTokenizer,
    onExtendedContextClick: (String) -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.study_session_answer_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
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
                engine = syntaxEngine
            )
        }
        val extendedContext = card.extendedContext
        if (!extendedContext.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            SuggestionChip(
                onClick = { onExtendedContextClick(extendedContext) },
                label = { Text(stringResource(R.string.study_session_extended_context_button)) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun CenteredBox(
    innerPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
