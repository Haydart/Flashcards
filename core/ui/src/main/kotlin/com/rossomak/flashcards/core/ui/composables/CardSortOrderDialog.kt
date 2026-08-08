package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.rossomak.flashcards.core.domain.model.CardSortOrder
import com.rossomak.flashcards.core.ui.R
import com.rossomak.flashcards.core.ui.theme.spacing

@Composable
fun CardSortOrderDialog(
    selectedSortOrder: CardSortOrder,
    showKeepAsDefaultOption: Boolean = false,
    onSortOrderSelect: (CardSortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    var keepAsDefault by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.card_sort_order_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                CardSortOrder.entries.forEach { sortOrder ->
                    SortOrderRow(
                        label = sortOrder.label(),
                        isSelected = sortOrder == selectedSortOrder,
                        onSelect = { onSortOrderSelect(sortOrder) },
                    )
                }
                if (showKeepAsDefaultOption) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xsmall))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = keepAsDefault,
                                onValueChange = { keepAsDefault = it },
                                role = Role.Checkbox,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = keepAsDefault,
                            onCheckedChange = null,
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xsmall))
                        Text(text = stringResource(R.string.card_sort_order_keep_as_default_label))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.common_done_button)) }
        },
    )
}

@Composable
private fun SortOrderRow(
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary),
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xsmall))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun CardSortOrder.label(): String = when (this) {
    CardSortOrder.Default -> stringResource(R.string.card_sort_order_default_label)
    CardSortOrder.EasiestFirst -> stringResource(R.string.card_sort_order_easiest_first_label)
    CardSortOrder.HardestFirst -> stringResource(R.string.card_sort_order_hardest_first_label)
}

@Preview
@Composable
private fun CardSortOrderDialogSessionPreview() {
    CardSortOrderDialog(
        selectedSortOrder = CardSortOrder.HardestFirst,
        showKeepAsDefaultOption = true,
        onSortOrderSelect = {},
        onDismiss = {},
    )
}

@Preview
@Composable
private fun CardSortOrderDialogSettingsPreview() {
    CardSortOrderDialog(
        selectedSortOrder = CardSortOrder.Default,
        showKeepAsDefaultOption = false,
        onSortOrderSelect = {},
        onDismiss = {},
    )
}
