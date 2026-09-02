package com.rossomak.flashcards.core.ui.composables.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.R
import com.rossomak.flashcards.core.ui.composables.FlashcardsDifficultyRangePill
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsTonalButton
import com.rossomak.flashcards.core.ui.composables.common.FlashcardsComponentSize
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * Max width of the [FlashcardsSettingRow.value] slot, so a long value can never starve the label
 * or the Edit button of space — see [FlashcardsSettingRow]'s `value` doc for the resulting
 * clipping contract. Local to this row shape, not a shared [com.rossomak.flashcards.core.ui.theme.AppSizes]
 * token: no second settings-row-shaped caller exists yet to confirm the number generalizes.
 */
private val SETTING_ROW_VALUE_MAX_WIDTH = 120.dp

/**
 * One adjustable setting in a settings sheet: `label … value [Edit]`, with a divider beneath.
 * Built on [FlashcardsListRow] for the minimum row height, layout and trailing slot; the row
 * itself carries no click — passed `onClick = null` to [FlashcardsListRow] so it attaches no
 * `clickable` at all, not just an inert one.
 *
 * **The Edit button is the tap target, not the row.** This deliberately departs from
 * (docs/adr/0030)'s "whole row is the tap target" framing: [onClick] is wired to a real, fully
 * interactive [FlashcardsTonalButton] — not a decorative hint — so it is the row's one and only
 * clickable element, with its own accessible label and click semantics. Tapping the row outside
 * the button does nothing.
 *
 * [value] is a composable slot, not a `String`, because not every setting's current value is text:
 * a Filters row needs to render a tag count next to [FlashcardsDifficultyRangePill], which a
 * `String` can't carry. Pass [valueText] instead for the common plain-text case and leave [value]
 * at its default — the call site stays a one-liner. Supplying [value] directly overrides
 * [valueText] entirely.
 *
 * [value] is width-capped at [SETTING_ROW_VALUE_MAX_WIDTH] so the Edit button never shrinks and
 * the label always keeps some space, even for an arbitrarily wide or long-localized value; a
 * custom [value] composable is clipped past that cap rather than auto-fit, so keep custom slots
 * (e.g. the tag-count-plus-pill shape) within it — a bare `Text` should still set its own
 * `maxLines`/`TextOverflow.Ellipsis` as the first line of defense before that hard clip.
 *
 * @param label What the setting is (e.g. "Mode", "Filters").
 * @param onClick Opens the setting's editor. Fires from the Edit button only.
 * @param valueText The setting's current value as plain text, rendered muted by the default
 *   [value]. Ignored once [value] is overridden.
 * @param value The setting's current value. Defaults to muted [valueText] text; override for
 *   non-text values.
 */
@Composable
fun FlashcardsSettingRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    value: @Composable () -> Unit = {
        if (valueText != null) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    },
) {
    Column(modifier = modifier) {
        FlashcardsListRow(
            title = label,
            onClick = null,
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    Box(modifier = Modifier.widthIn(max = SETTING_ROW_VALUE_MAX_WIDTH).weight(1f, fill = false)) {
                        value()
                    }
                    FlashcardsTonalButton(
                        text = stringResource(R.string.common_edit_button),
                        onClick = onClick,
                        size = FlashcardsComponentSize.Small,
                        icon = Icons.Default.Edit,
                    )
                }
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@ShowkaseComposable(name = "Setting row", group = "Lists")
@Composable
fun FlashcardsSettingRowShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsSettingRow(label = "Mode", valueText = "Rated", onClick = {})
        }
    }
}

@ShowkaseComposable(name = "Setting row — custom value", group = "Lists")
@Composable
fun FlashcardsSettingRowCustomValueShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsSettingRow(label = "Filters", onClick = {}, value = { FiltersSampleValue() })
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsSettingRowPreview() {
    FlashcardsTheme {
        Surface {
            Column {
                FlashcardsSettingRow(label = "Mode", valueText = "Rated", onClick = {})
                FlashcardsSettingRow(label = "Length", valueText = "18 cards", onClick = {})
                FlashcardsSettingRow(label = "Filters", onClick = {}, value = { FiltersSampleValue() })
            }
        }
    }
}

/** Sample non-text [FlashcardsSettingRow.value]: a tag count next to [FlashcardsDifficultyRangePill]. */
@Composable
private fun FiltersSampleValue() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
    ) {
        Text(
            text = "2 tags",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlashcardsDifficultyRangePill(lowLevel = 2, highLevel = 4)
    }
}
