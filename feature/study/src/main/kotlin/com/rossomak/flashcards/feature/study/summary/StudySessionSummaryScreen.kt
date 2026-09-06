package com.rossomak.flashcards.feature.study.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.feature.study.R

/**
 * A Rated or Fast Study Session's mandatory egress — natural end or premature exit alike (spec 03
 * ticket 02). Deliberately plain: no scoring, no animation, no "Study Again" actions. Those land
 * once the content they depend on actually exists (spec 05), so this screen is not designed twice.
 *
 * [onNavigateBack] is this screen's one action, and system back from it does the same thing — both
 * pop straight to the tab the user started from, since `NavGraph.kt` already replaced everything
 * between them on the back stack when it navigated here.
 */
@Composable
fun StudySessionSummaryScreen(
    modifier: Modifier = Modifier,
    viewModel: StudySessionSummaryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StudySessionSummaryContent(modifier = modifier, state = state, onNavigateBack = onNavigateBack)
}

@Composable
fun StudySessionSummaryContent(
    modifier: Modifier = Modifier,
    state: StudySessionSummaryScreenState,
    onNavigateBack: () -> Unit,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(
                    if (state.mode == StudyMode.Rated) {
                        R.string.study_session_summary_mode_rated_label
                    } else {
                        R.string.study_session_summary_mode_fast_label
                    },
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(
                    if (state.abandoned) {
                        R.string.study_session_summary_abandoned_label
                    } else {
                        R.string.study_session_summary_completed_label
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    R.string.study_session_summary_duration_label,
                    state.durationSeconds / SECONDS_PER_MINUTE,
                    state.durationSeconds % SECONDS_PER_MINUTE,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.study_session_summary_cards_studied_label, state.studiedCount),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (state.mode == StudyMode.Rated) {
                Text(
                    text = stringResource(R.string.study_session_summary_mastered_count_label, state.masteredCount),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.study_session_summary_partial_count_label, state.partialCount),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.study_session_summary_failed_count_label, state.failedCount),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Button(onClick = onNavigateBack) {
                Text(stringResource(R.string.study_session_summary_back_button))
            }
        }
    }
}

private const val SECONDS_PER_MINUTE = 60
