package com.rossomak.flashcards.feature.study.chrome

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.feature.study.R

/**
 * The top app bar shared by every Study Session screen (ticket 01 of
 * [ADR-0045](../../../../../../../../docs/adr/0045-separate-fast-and-rated-session-screens.md)):
 * a close action that opens the exit-confirmation dialog, a flag action shown only while a card is
 * on screen that opens Report a problem for that card, and a trailing counter shown whenever
 * [counterText] is non-null.
 *
 * Takes the current card and the already-formatted counter text as plain values rather than the
 * screen state that owns them — the two Study Modes are about to diverge into two different state
 * types, and this bar must not force either of them into a shared supertype just to be fed. The
 * counter's meaning itself diverges too (ticket 03 of the Rated session state machine sequence):
 * Fast shows deck position, Rated shows distinct cards mastered — so this bar renders [counterText]
 * verbatim rather than deriving it from a position/total pair, and takes no Study Mode flag to
 * choose between them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionTopAppBar(
    modifier: Modifier = Modifier,
    sessionTitle: String,
    reportableCard: Flashcard?,
    counterText: String?,
    onClose: () -> Unit,
    onReportProblem: (card: Flashcard) -> Unit,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = sessionTitle) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.exit_session_dialog_title),
                )
            }
        },
        actions = {
            if (reportableCard != null) {
                IconButton(
                    onClick = { onReportProblem(reportableCard) },
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = stringResource(R.string.report_problem_dialog_title),
                    )
                }
            }
            if (counterText != null) {
                Text(
                    text = counterText,
                    modifier = Modifier.padding(end = 16.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}
