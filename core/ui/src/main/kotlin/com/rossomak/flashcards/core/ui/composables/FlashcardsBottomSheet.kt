package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsFilledButton
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * A non-modal bottom sheet docked wherever the caller places it — built on M3's standalone
 * `BottomSheet`, not `ModalBottomSheet` or `BottomSheetScaffold`. It renders in the composition
 * hierarchy rather than a separate dialog window, so there is no scrim: the caller decides what
 * else is visible while the sheet is open. See docs/adr/0043 for why this shape was chosen over
 * the scaffold and the modal sheet.
 *
 * Deliberately not a scaffold and deliberately thin: it owns no `topBar`, no header slot and no
 * pinned-actions region — most sheets built on this will not need pinned actions at all, and the
 * caller is better placed to lay out its own [content]. Dock it the same way the old panel was
 * docked: `Box(Modifier.fillMaxSize()) { ScreenContent(); FlashcardsBottomSheet(state = ...,
 * modifier = Modifier.align(Alignment.BottomCenter)) { ... } }`.
 *
 * [state] is built with [rememberFlashcardsBottomSheetState], which only ever anchors the sheet
 * at hidden or expanded — there is no partially-expanded state to reason about and no peek height
 * to compute. Predictive back, the bottom system-bar inset, and the drag handle's
 * expand/collapse/dismiss accessibility actions all come from `BottomSheet` itself; none of it is
 * re-implemented here, and callers add no `navigationBarsPadding` of their own.
 *
 * **Height is the caller's problem.** The expanded anchor is derived from [content]'s measured
 * height: content taller than the screen pins at the top and *clips* rather than scrolling. A
 * caller with variable-length content is expected to make its own content column scrollable — no
 * percentage ceiling is imposed here.
 *
 * @param state Owns the sheet's hidden/expanded value. Build it with
 *   [rememberFlashcardsBottomSheetState].
 * @param onDismissRequest Invoked when the sheet is swiped or predictive-backed to hidden.
 *   Ignored when [dismissible] is `false`, since neither path can reach hidden in that case.
 * @param draggable Whether drag gestures move the sheet. The drag handle's tap-to-toggle and
 *   accessibility actions stay available either way.
 * @param dismissible Whether the sheet can reach the hidden value at all. When `false`, swipe and
 *   predictive back are both disabled for this sheet, matching the hidden/expanded set
 *   [state] was built with — never drive a non-dismissible sheet's [state] toward hidden.
 * @param content The sheet's body. One slot, no header, no pinned-actions region.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsBottomSheet(
    state: SheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    draggable: Boolean = true,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    BottomSheet(
        state = state,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        gesturesEnabled = draggable,
        backHandlerEnabled = dismissible,
        shape = RoundedCornerShape(
            topStart = MaterialTheme.cornerRadius.large,
            topEnd = MaterialTheme.cornerRadius.large,
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        content = content,
    )
}

/**
 * Builds the [SheetState] [FlashcardsBottomSheet] expects: hidden and expanded only, never
 * partially expanded.
 *
 * When [dismissible] is `false`, hidden is dropped from the enabled set entirely — not just
 * blocked at the [FlashcardsBottomSheet] call site — so the state starts at expanded and calling
 * [SheetState.hide] on it throws, per M3's own contract. A `confirmValueChange` veto is also
 * attached, rejecting a settle at hidden, since `BottomSheet`'s drag physics always define a
 * hidden anchor for the gesture to rubber-band against, independent of the enabled set.
 *
 * @param dismissible Whether hidden is one of the sheet's enabled values.
 * @param initiallyExpanded Whether a dismissible sheet starts expanded instead of hidden. Ignored
 *   when [dismissible] is `false`, which always starts expanded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberFlashcardsBottomSheetState(
    dismissible: Boolean = true,
    initiallyExpanded: Boolean = false,
): SheetState {
    val enabledValues = if (dismissible) {
        setOf(SheetValue.Hidden, SheetValue.Expanded)
    } else {
        setOf(SheetValue.Expanded)
    }
    val initialValue = if (dismissible && !initiallyExpanded) SheetValue.Hidden else SheetValue.Expanded
    return rememberBottomSheetState(
        initialValue = initialValue,
        enabledValues = enabledValues,
        confirmValueChange = { value -> dismissible || value != SheetValue.Hidden },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@ShowkaseComposable(name = "Bottom sheet", group = "Sheets")
@Composable
fun FlashcardsBottomSheetShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsBottomSheet(
                state = rememberFlashcardsBottomSheetState(initiallyExpanded = true),
                onDismissRequest = {},
            ) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun FlashcardsBottomSheetPreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsBottomSheet(
                state = rememberFlashcardsBottomSheetState(initiallyExpanded = true),
                onDismissRequest = {},
            ) {
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
}
