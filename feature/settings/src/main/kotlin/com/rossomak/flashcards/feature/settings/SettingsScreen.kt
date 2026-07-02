package com.rossomak.flashcards.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.ui.composables.VoiceSettingsDialog

@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateToLogin) {
        if (state.navigateToLogin) {
            onNavigateToLogin()
        }
    }

    if (state.voiceSettingsState.isVisible) {
        VoiceSettingsDialog(
            availableVoices = state.voiceSettingsState.availableVoices,
            selectedVoiceId = state.voiceSettingsState.draftVoiceId,
            speechRate = state.voiceSettingsState.draftSpeed,
            onVoiceSelected = viewModel::onVoiceSettingsDraftVoiceChanged,
            onSpeedChanged = viewModel::onVoiceSettingsDraftSpeedChanged,
            onSave = viewModel::onVoiceSettingsSave,
            onDismiss = viewModel::onVoiceSettingsDismiss,
        )
    }

    SettingsContent(
        isSigningOut = state.isSigningOut,
        onVoicePlaybackSettingsClick = viewModel::onVoicePlaybackSettingsClick,
        onSignOutClick = viewModel::onSignOutClick,
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    isSigningOut: Boolean,
    onVoicePlaybackSettingsClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Settings - NYI")
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onVoicePlaybackSettingsClick) {
            Text(text = "Voice playback settings")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onSignOutClick, enabled = !isSigningOut) {
            if (isSigningOut) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Signing out...")
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Sign out")
            }
        }
    }
}
