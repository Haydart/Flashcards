package com.rossomak.flashcards.core.domain.model

/**
 * @param displayName The disambiguating label for a picker listing every option side by side
 *   (e.g. [VoiceSettingsDialog][com.rossomak.flashcards.core.ui.composables.dialogs.VoiceSettingsDialog])
 *   — includes whatever the source needs to tell same-sounding options apart (for TTS voices, the
 *   country plus the engine's own variant id, since multiple voices can share a country).
 * @param shortLabel The compact label for a summary naming only the *current* selection, with
 *   nothing beside it to disambiguate against (e.g. a settings row). Defaults to [displayName] —
 *   only worth diverging from when [displayName] carries disambiguating detail (like a raw engine
 *   id) that reads as noise once nothing else is competing for attention.
 */
data class VoiceOption(val id: String, val displayName: String, val shortLabel: String = displayName)
