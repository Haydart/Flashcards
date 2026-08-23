package com.rossomak.flashcards.feature.settings.gallery

import com.rossomak.flashcards.core.domain.model.VoiceOption

/**
 * Static sample data the gallery feeds its dialogs. Debug-only, so it deliberately has no
 * repository behind it — the point of the harness is the dialog behavior, not the data.
 */
internal object GalleryFixtures {

    val SESSION_LENGTH_RANGE = 10..50

    const val SESSION_LENGTH_STEP = 5

    const val DEFAULT_SESSION_LENGTH = 20

    val DIFFICULTY_BOUNDS = 1..10

    val DEFAULT_DIFFICULTY_RANGE = 3..8

    const val DEFAULT_SPEECH_RATE = 1.25f

    val TAGS = listOf("state", "recomposition", "side-effects", "modifiers", "navigation", "testing")

    val VOICES = listOf(
        VoiceOption(id = "en-us-x-1", displayName = "Google en-US · Female"),
        VoiceOption(id = "en-gb-x-1", displayName = "Google en-GB · Male"),
        VoiceOption(id = "en-au-x-1", displayName = "Google en-AU · Female"),
    )

    val CODE_SAMPLE = """
        // survives config changes
        @Composable
        fun Counter() {
            var count by rememberSaveable { mutableIntStateOf(0) }
        }
    """.trimIndent()
}
