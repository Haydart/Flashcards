package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.SessionCommit

/**
 * Persists a finished Study Session, once, at the Session Summary screen
 * ([ADR-0014](../../../../../../../docs/adr/0014-session-stats-written-at-summary-screen.md)).
 *
 * The whole save is one Firestore batch. Ticket 01 of spec 04 wrote only the `sessions/{sessionId}`
 * document; ticket 02 adds the packed progress documents to that same batch, ticket 03 the
 * progress-summary increments, and spec 05 a further scoring-state write — none of that changes this
 * signature, only what [SessionCommit] itself carries.
 *
 * @param onRejected invoked, asynchronously and at most once, only if the batch is genuinely
 * rejected after being accepted locally — never for a write that is merely queued offline. This
 * method does not await that outcome (see ADR-0014's Offline section): it returns as soon as the
 * write is handed to Firestore, so an offline caller sees success immediately.
 */
interface StudySessionRepository {

    suspend fun commitSession(sessionCommit: SessionCommit, onRejected: (Throwable) -> Unit = {}): Result<Unit>
}
