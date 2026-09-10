package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.SessionResult

/**
 * Delivers a finished session to the server-authoritative `submitStudySession` Cloud Function,
 * which becomes the sole writer of the session's XP, level and progress —
 * `sessions/{sessionId}`, `progress/details/subcategories/{subcategoryId}`, `progress/summary` and
 * `progress/user-stats` are no longer written by this client at all. This is a pure submission:
 * nothing it returns carries write authority, and [com.rossomak.flashcards.core.domain.usecase.SubmitStudySessionUseCase]'s
 * own optimistic preview is never reconciled against what the function actually computed
 * (deliberately none — every other screen just reads the same documents the
 * function itself writes, the next time it is viewed).
 *
 * Named "submission", not "report": this codebase's curation feature already owns "report" for a
 * flagged-content signal ([com.rossomak.flashcards.core.domain.usecase.SubmitCurationReportUseCase]),
 * so a finished session is *submitted*, never *reported*, to keep the two concepts from colliding.
 *
 * A durable, WorkManager-backed retry queue wraps this so an offline or killed-app
 * submission still lands once connectivity returns; this interface's own contract does not change
 * when that lands, only what calls it does.
 */
interface SessionSubmissionRepository {

    suspend fun submitSession(sessionResult: SessionResult): Result<Unit>
}
