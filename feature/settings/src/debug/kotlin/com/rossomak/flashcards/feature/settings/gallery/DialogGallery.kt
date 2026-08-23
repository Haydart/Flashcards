package com.rossomak.flashcards.feature.settings.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsOutlinedButton
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsTonalButton
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFilters
import com.rossomak.flashcards.core.ui.theme.spacing
import com.rossomak.flashcards.feature.settings.R

/**
 * Developer harness listing every dialog in the design system.
 *
 * Exists because the dialogs are built before the screens that will host them, so this is the only
 * way to exercise them. It is not a component browser like Showkase: dialogs are *behavioral*, and
 * what needs checking is that a scrim tap discards, that a decision dialog ignores the scrim, that
 * Submit stays disabled until something is checked. Static previews cannot show any of that.
 *
 * The harness deliberately uses the production state pattern — one sealed [GalleryDialog] field, a
 * single `onDialogEvent` callback, and [DialogGalleryHost] holding the exhaustive `when` — so the
 * architecture is exercised alongside the visuals. The one difference is that the state lives in a
 * `remember` rather than a ViewModel, since there is nothing here to persist.
 *
 * Debug source set only; the release variant of this object renders nothing.
 */
internal object DialogGallery {

    const val IS_AVAILABLE = true

    /** Label for the Settings entry point. Lives here so no gallery string reaches release res. */
    @Composable
    fun EntryLabel(): String = stringResource(R.string.dialog_gallery_button)

    @Composable
    fun Content(
        onClose: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        var activeDialog by remember { mutableStateOf<GalleryDialog?>(null) }
        var lastResult by remember { mutableStateOf<String?>(null) }

        val confirmedLabel = stringResource(R.string.dialog_gallery_result_confirmed_label)
        val dismissedLabel = stringResource(R.string.dialog_gallery_result_dismissed_label)

        val onDialogEvent: (GalleryDialogEvent) -> Unit = { event ->
            when (event) {
                GalleryDialogEvent.Confirm -> lastResult = confirmedLabel
                GalleryDialogEvent.Dismiss -> lastResult = dismissedLabel
                else -> Unit
            }
            activeDialog = activeDialog.reduce(event)
        }

        DialogGalleryHost(activeDialog = activeDialog, onDialogEvent = onDialogEvent)

        Surface(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(MaterialTheme.spacing.normal),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
            ) {
                Text(
                    text = stringResource(R.string.dialog_gallery_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.dialog_gallery_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (lastResult != null) {
                    Text(
                        text = stringResource(R.string.dialog_gallery_last_result_label, lastResult.orEmpty()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                GalleryEntry(
                    labelRes = R.string.dialog_gallery_sort_button,
                    dialog = GalleryDialog.Sort(draft = FlashcardSortOrder.HardestFirst),
                    onDialogEvent = onDialogEvent,
                )
                GalleryEntry(
                    labelRes = R.string.dialog_gallery_mode_button,
                    dialog = GalleryDialog.Mode(draft = StudyMode.Rated),
                    onDialogEvent = onDialogEvent,
                )
                GalleryEntry(
                    labelRes = R.string.dialog_gallery_length_button,
                    dialog = GalleryDialog.Length(draft = GalleryFixtures.DEFAULT_SESSION_LENGTH),
                    onDialogEvent = onDialogEvent,
                )
                GalleryEntry(
                    labelRes = R.string.dialog_gallery_voice_answering_button,
                    dialog = GalleryDialog.VoiceAnswering(draft = true),
                    onDialogEvent = onDialogEvent,
                )
                GalleryEntry(
                    labelRes = R.string.dialog_gallery_voice_button,
                    dialog = GalleryDialog.Voice(
                        draftVoiceId = GalleryFixtures.VOICES.first().id,
                        draftSpeechRate = GalleryFixtures.DEFAULT_SPEECH_RATE,
                        availableVoices = GalleryFixtures.VOICES,
                    ),
                    onDialogEvent = onDialogEvent,
                )
                GalleryEntry(
                    labelRes = R.string.dialog_gallery_filters_button,
                    dialog = GalleryDialog.Filters(
                        draft = FlashcardFilters(
                            selectedTags = setOf(GalleryFixtures.TAGS.first()),
                            difficultyRange = GalleryFixtures.DEFAULT_DIFFICULTY_RANGE,
                        ),
                    ),
                    onDialogEvent = onDialogEvent,
                )
                GalleryEntry(
                    labelRes = R.string.dialog_gallery_report_button,
                    dialog = GalleryDialog.Report(selectedActions = emptySet()),
                    onDialogEvent = onDialogEvent,
                )
                GalleryEntry(
                    labelRes = R.string.dialog_gallery_extended_context_button,
                    dialog = GalleryDialog.ExtendedContext,
                    onDialogEvent = onDialogEvent,
                )
                GalleryEntry(
                    labelRes = R.string.dialog_gallery_exit_button,
                    dialog = GalleryDialog.ExitSession,
                    onDialogEvent = onDialogEvent,
                )
                FlashcardsTonalButton(
                    text = stringResource(R.string.dialog_gallery_close_button),
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.normal),
                )
            }
        }
    }
}

@Composable
private fun GalleryEntry(
    labelRes: Int,
    dialog: GalleryDialog,
    onDialogEvent: (GalleryDialogEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlashcardsOutlinedButton(
        text = stringResource(labelRes),
        onClick = { onDialogEvent(GalleryDialogEvent.Open(dialog)) },
        modifier = modifier.fillMaxWidth(),
    )
}
