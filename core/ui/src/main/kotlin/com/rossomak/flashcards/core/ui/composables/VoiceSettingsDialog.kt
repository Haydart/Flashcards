package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rossomak.flashcards.core.domain.model.VoiceOption
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsDialog(
    availableVoices: List<VoiceOption>,
    selectedVoiceId: String?,
    speechRate: Float,
    onVoiceSelected: (String?) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val selectedVoice = availableVoices.firstOrNull { it.id == selectedVoiceId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Voice settings") },
        text = {
            Column {
                Text(
                    text = "VOICE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedVoice?.displayName ?: "Select voice",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        availableVoices.forEach { voice ->
                            DropdownMenuItem(
                                text = { Text(voice.displayName) },
                                onClick = {
                                    dropdownExpanded = false
                                    onVoiceSelected(voice.id)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPEED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", speechRate)}×",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Slider(
                    value = speechRate,
                    onValueChange = onSpeedChanged,
                    valueRange = 0.5f..2f,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "0.5×",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "2.0×",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Changes apply permanently to user preferences, not just the current session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview
@Composable
private fun VoiceSettingsDialogNoSelectionPreview() {
    VoiceSettingsDialog(
        availableVoices = listOf(
            VoiceOption(id = "en-us-x-1", displayName = "English (United States) · Voice 1"),
            VoiceOption(id = "en-gb-x-1", displayName = "English (United Kingdom) · Voice 1")
        ),
        selectedVoiceId = null,
        speechRate = 1f,
        onVoiceSelected = {},
        onSpeedChanged = {},
        onSave = {},
        onDismiss = {}
    )
}

@Preview
@Composable
private fun VoiceSettingsDialogVoiceSelectedPreview() {
    VoiceSettingsDialog(
        availableVoices = listOf(
            VoiceOption(id = "en-us-x-1", displayName = "English (United States) · Voice 1"),
            VoiceOption(id = "en-gb-x-1", displayName = "English (United Kingdom) · Voice 1")
        ),
        selectedVoiceId = "en-gb-x-1",
        speechRate = 1.5f,
        onVoiceSelected = {},
        onSpeedChanged = {},
        onSave = {},
        onDismiss = {}
    )
}

@Preview
@Composable
private fun VoiceSettingsDialogEmptyVoicesPreview() {
    VoiceSettingsDialog(
        availableVoices = emptyList(),
        selectedVoiceId = null,
        speechRate = 1f,
        onVoiceSelected = {},
        onSpeedChanged = {},
        onSave = {},
        onDismiss = {}
    )
}
