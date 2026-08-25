package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsFilledButton
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * A permanently docked panel pinned to the bottom of a screen — not M3's `ModalBottomSheet` or
 * `BottomSheetScaffold`. There is no scrim, no swipe-to-dismiss, and no way for the user to hide
 * it: the caller decides what's on screen by changing [content], never by the sheet being open or
 * closed. Use it for the "config summary + start button" panels docked under a study preview, and
 * any future screen that wants the same permanently-visible bottom panel.
 *
 * Deliberately not a scaffold: it does not own a `topBar` or a main-content slot the way
 * `BottomSheetScaffold` does. The caller docks it themselves — typically `Box(Modifier.fillMaxSize())
 * { ScreenContent(); FlashcardsBottomSheet(modifier = Modifier.align(Alignment.BottomCenter)) { ... } }`
 * — so it composes into any screen's existing layout instead of replacing it.
 *
 * [minHeight] is a floor, not a fixed size: [content] can grow the sheet taller (e.g. StudySessionScreen's
 * panel is taller once an answer is revealed), it just never shrinks below it. Content padding
 * ([MaterialTheme.spacing.normal]) and the navigation-bar inset are applied internally, so callsites
 * don't each have to remember them.
 */
@Composable
fun FlashcardsBottomSheet(
    minHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        shape = RoundedCornerShape(
            topStart = MaterialTheme.cornerRadius.large,
            topEnd = MaterialTheme.cornerRadius.large,
        ),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(MaterialTheme.spacing.normal),
            content = content,
        )
    }
}

@ShowkaseComposable(name = "Bottom sheet", group = "Sheets")
@PreviewLightDark
@Composable
private fun FlashcardsBottomSheetShowcase() {
    FlashcardsTheme {
        FlashcardsBottomSheet(minHeight = 152.dp) {
            Text(text = "Mode: Rated", style = MaterialTheme.typography.bodyLarge)
            Text(text = "18 cards", style = MaterialTheme.typography.bodyMedium)
            FlashcardsFilledButton(
                text = "Start session",
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.normal),
            )
        }
    }
}
