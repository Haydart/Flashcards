package com.rossomak.flashcards.feature.settings

import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.ui.composables.VoiceSettingsDialog
import com.rossomak.flashcards.core.ui.showcase.Showcase
import com.rossomak.flashcards.core.ui.navigation.ObserveAsEvents
import leakcanary.AppWatcher

/**
 * Process-static sink used only to demonstrate LeakCanary in debug builds. Holding a hard
 * reference here keeps each watched object from becoming weakly reachable, so LeakCanary's
 * retained-object check (~5s after [AppWatcher.objectWatcher.expectWeaklyReachable]) reports it
 * as a leak without needing an actual Activity destroy/recreate to happen first.
 */
private object LeakCanaryTestSink {
    val leakedReferences = mutableListOf<Any>()
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { destination ->
        when (destination) {
            SettingsDestination.Login -> onNavigateToLogin()
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

    val showcaseIntent = remember { Showcase.intentOrNull(context) }

    SettingsContent(
        isSigningOut = state.isSigningOut,
        showcaseIntent = showcaseIntent,
        onVoicePlaybackSettingsClick = viewModel::onVoicePlaybackSettingsClick,
        onSignOutClick = viewModel::onSignOutClick,
        onTriggerMemoryLeakClick = if (BuildConfig.DEBUG) {
            {
                val leaked = Any()
                LeakCanaryTestSink.leakedReferences.add(leaked)
                AppWatcher.objectWatcher.expectWeaklyReachable(leaked, "Manually triggered from Settings screen")
            }
        } else {
            null
        },
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    isSigningOut: Boolean,
    showcaseIntent: Intent?,
    onVoicePlaybackSettingsClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onTriggerMemoryLeakClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

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
        if (showcaseIntent != null) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = { context.startActivity(showcaseIntent) }) {
                Text(text = "UI component showcase")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (onTriggerMemoryLeakClick != null) {
            OutlinedButton(onClick = onTriggerMemoryLeakClick) {
                Text(text = "Test LeakCanary (leak activity)")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
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
