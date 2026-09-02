package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.R
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsIconButton
import com.rossomak.flashcards.core.ui.composables.common.FlashcardsComponentSize
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * A title with a trailing close button, for a [FlashcardsBottomSheet] that can be dismissed by tap
 * as well as by gesture. Not redundant with the sheet's own drag handle: the handle is the drag
 * affordance, this X is the tap affordance, and a user who taps the handle gets nothing.
 *
 * The close control is [FlashcardsIconButton] at its `Small` size, from ticket 01's circular icon
 * button family.
 *
 * @param title The sheet's title.
 * @param onClose Dismisses the sheet — typically driving its `SheetState` toward hidden.
 */
@Composable
fun FlashcardsBottomSheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        FlashcardsIconButton(
            icon = Icons.Default.Close,
            contentDescription = stringResource(R.string.common_close_cd),
            onClick = onClose,
            size = FlashcardsComponentSize.Small,
        )
    }
}

@ShowkaseComposable(name = "Bottom sheet header", group = "Sheets")
@Composable
fun FlashcardsBottomSheetHeaderShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsBottomSheetHeader(title = "Session settings", onClose = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsBottomSheetHeaderPreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsBottomSheetHeader(title = "Session settings", onClose = {})
        }
    }
}
