package com.rossomak.flashcards.feature.study.summary

/**
 * One-shot snackbar messages from the Session Summary, per the SharedFlow-for-transient-events rule
 * (ADR-0019). Never screen state: the displayed results stay on screen regardless, nothing is
 * cleared and nothing navigates (ADR-0014's Offline section).
 */
sealed interface StudySessionSummaryMessage {

    /**
     * The session commit was rejected — a genuine failure, not a queued offline write. A queued
     * write shows no message at all: it will land once connectivity returns.
     */
    data object SaveFailed : StudySessionSummaryMessage
}
