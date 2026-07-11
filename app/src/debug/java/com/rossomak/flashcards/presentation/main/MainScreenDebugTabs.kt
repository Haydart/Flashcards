package com.rossomak.flashcards.presentation.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.rossomak.flashcards.feature.voicedebug.R as VoiceDebugR
import com.rossomak.flashcards.feature.voicedebug.VoiceDebugGraph
import com.rossomak.flashcards.feature.voicedebug.VoiceDebugRoot
import com.rossomak.flashcards.feature.voicedebug.VoiceDebugScreen

@Composable
internal fun debugTabs(): List<TabItem> = listOf(
    TabItem(stringResource(VoiceDebugR.string.main_voice_debug_tab_label), Icons.Filled.GraphicEq, VoiceDebugGraph)
)

internal fun NavGraphBuilder.debugNavGraphEntries() {
    navigation<VoiceDebugGraph>(startDestination = VoiceDebugRoot) {
        composable<VoiceDebugRoot> {
            VoiceDebugScreen()
        }
    }
}
