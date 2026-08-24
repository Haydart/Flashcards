package com.rossomak.flashcards.feature.onboarding.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

// TODO(core-ui): replace with the extended radio card being added to :core:ui. Signature is
//  deliberately shaped like that component (leading slot, title, description, selected, onSelect)
//  so adoption is an import change rather than a layout rewrite.

/** Border width of the selected card — heavier than the hairline so selection reads at a glance. */
private val SelectedBorderWidth = 2.dp

/**
 * A tall radio card: leading glyph, title, and a full sentence of explanation. Taller and wordier
 * than the equivalent control on the Preview Study Session Screen because this is a first
 * explanation of the mode, not a reminder to someone who already knows it.
 *
 * The whole card is one `selectable` node with [Role.RadioButton], so the inner [RadioButton] takes
 * `onClick = null` and TalkBack announces the card once rather than announcing a separate control.
 */
@Composable
fun OnboardingModeCard(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        shape = RoundedCornerShape(MaterialTheme.cornerRadius.card),
        color = MaterialTheme.colorScheme.surface,
        border = if (selected) {
            BorderStroke(SelectedBorderWidth, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(MaterialTheme.sizes.hairline, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.normal),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            leading?.invoke()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxsmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.align(Alignment.Top),
            )
        }
    }
}
