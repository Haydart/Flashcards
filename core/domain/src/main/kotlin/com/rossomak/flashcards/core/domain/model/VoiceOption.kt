package com.rossomak.flashcards.core.domain.model

/**
 * A curated TTS voice. Carries only platform-derived, opaque data — no user-facing label at all.
 * `Voice.name` (Android's own voice identifier, surfaced here as [id]) is documented as unique but
 * *not* as any particular format; parsing it for display text is not safe (a name with no
 * delimiter, or two distinct voices sharing whatever suffix a naive parse extracts, both produce a
 * bad label). A friendly label is built instead from [countryCode] and [variantIndex] by the layer
 * that owns user-facing strings (`core:ui`'s `VoiceOption.label()`) — the domain layer has no
 * Android dependencies (AGENTS.md), so it cannot hold a formatted string itself.
 *
 * @param id The platform's own unique voice identifier (`Voice.name`), persisted as the user's
 *   selection and never shown directly.
 * @param countryCode The voice's ISO country code (e.g. "US"), one of the curator's preferred set.
 * @param variantIndex This voice's 1-based position among every curated voice sharing [countryCode]
 *   — a user-friendly "Voice 1", "Voice 2" disambiguates same-country voices without ever touching
 *   the opaque platform id.
 */
data class VoiceOption(val id: String, val countryCode: String, val variantIndex: Int)
