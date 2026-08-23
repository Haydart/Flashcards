package com.rossomak.flashcards.feature.settings.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Release stand-in for the debug dialog gallery, mirroring the `Showcase` object's debug/release
 * split in `:core:ui`. [IS_AVAILABLE] is `false`, so `SettingsScreen` never offers the entry point
 * and [Content] is never reached — and the `:feature:study` dependency the debug variant needs
 * (`debugImplementation`) is absent from release builds entirely.
 */
internal object DialogGallery {

    const val IS_AVAILABLE = false

    /** Never rendered: [IS_AVAILABLE] is `false`, so the Settings entry point is not shown. */
    @Composable
    fun EntryLabel(): String = ""

    @Composable
    @Suppress("UnusedParameter")
    fun Content(
        onClose: () -> Unit,
        modifier: Modifier = Modifier,
    ) = Unit
}
