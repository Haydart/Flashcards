package com.rossomak.flashcards.core.domain.model

/**
 * One writable [StudySessionPreferences] field per case, so Settings and Preview write exactly
 * the preference their dialog confirmed instead of a read-modify-write of the whole object.
 */
sealed interface StudySessionPreference {
    data class DefaultStudyMode(val value: StudyMode) : StudySessionPreference

    data class VoiceAnsweringEnabled(val value: Boolean) : StudySessionPreference

    data class RatedAttempts(val value: Int) : StudySessionPreference

    data class ReadAloudEnabled(val value: Boolean) : StudySessionPreference

    data class SessionLength(val value: Int) : StudySessionPreference

    data class SortOrder(val value: FlashcardSortOrder) : StudySessionPreference
}
