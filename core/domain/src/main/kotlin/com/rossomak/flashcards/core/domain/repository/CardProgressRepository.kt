package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.SubcategoryProgress

/**
 * Reads a User's packed per-Subcategory progress document
 * ([ADR-0016](../../../../../../../docs/adr/0016-card-progress-model.md)). The session commit is
 * one caller — this read is what tells it which cards are new and which were previously
 * mastered — but it also serves Subcategory Details and the Preview screen's defense selection,
 * which is why this lives in `core:domain` rather than inside `feature:study`.
 *
 * Writing is not exposed here: a session commit's progress writes must land in the same Firestore
 * batch as its session document, so they are expressed as [com.rossomak.flashcards.core.domain.model.SubcategoryProgressWrite]s
 * carried on [com.rossomak.flashcards.core.domain.model.SessionCommit] and applied by
 * [StudySessionRepository.commitSession] instead.
 */
interface CardProgressRepository {
    /** `null` when the User has never studied a card in this Subcategory yet. */
    suspend fun getProgress(subcategoryId: String): Result<SubcategoryProgress?>
}
