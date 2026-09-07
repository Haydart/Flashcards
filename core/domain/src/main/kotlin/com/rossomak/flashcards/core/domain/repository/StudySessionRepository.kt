package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.SessionResult

/**
 * Persists a finished Study Session, once, at the Session Summary screen
 * ([ADR-0014](../../../../../../../docs/adr/0014-session-stats-written-at-summary-screen.md)).
 *
 * The whole save is one Firestore batch. Ticket 01 of spec 04 writes only the `sessions/{sessionId}`
 * document; tickets 02 and 03 add the packed progress documents and the progress-summary increments
 * to that same batch, and spec 05 a further scoring-state write — none of that changes this
 * signature, only what the implementation's batch carries.
 *
 * @param onRejected invoked, asynchronously and at most once, only if the batch is genuinely
 * rejected after being accepted locally — never for a write that is merely queued offline. This
 * method does not await that outcome (see ADR-0014's Offline section): it returns as soon as the
 * write is handed to Firestore, so an offline caller sees success immediately.
 */
interface StudySessionRepository {

    suspend fun commitSession(sessionResult: SessionResult, onRejected: (Throwable) -> Unit = {}): Result<Unit>
}
