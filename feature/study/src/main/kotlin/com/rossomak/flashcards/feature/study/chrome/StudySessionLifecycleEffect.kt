package com.rossomak.flashcards.feature.study.chrome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Tells a Study Session ViewModel its own screen's lifecycle rather than the ViewModel observing
 * one itself, so the session clock stays testable without a device (spec 03 tickets 02/03 — Rated
 * and Fast both use this, sharing no base class per ADR-0045). `ON_STOP`/`ON_START` bracket the app
 * truly leaving/returning to the foreground — unlike `ON_PAUSE`/`ON_RESUME`, which also fire for a
 * same-Activity overlay (a system permission prompt, say) that never actually backgrounds the
 * session.
 */
@Composable
fun ObserveStudySessionLifecycle(onBackgrounded: () -> Unit, onForegrounded: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBackgrounded = rememberUpdatedState(onBackgrounded)
    val currentOnForegrounded = rememberUpdatedState(onForegrounded)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> currentOnBackgrounded.value()
                Lifecycle.Event.ON_START -> currentOnForegrounded.value()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
