package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.ProgressSummary
import com.rossomak.flashcards.core.domain.model.SubcategoryProgress

/**
 * Reads a User's packed per-Subcategory progress document and their per-user progress-summary
 * singleton (both [ADR-0016](../../../../../../../docs/adr/0016-card-progress-model.md)). The
 * session commit is one caller of [getProgress] — this read is what tells it which cards are new
 * and which were previously mastered — but it also serves Subcategory Details and the Preview
 * screen's defense selection; [getProgressSummary] serves Category Details' and the Home screen's
 * progress rings (spec 06). Both live in `core:domain` rather than inside a single feature module
 * because more than one feature reads each.
 *
 * Writing is not exposed here: a session commit's progress and summary writes must land in the same
 * Firestore batch as its session document, so they are expressed as
 * [com.rossomak.flashcards.core.domain.model.SubcategoryProgressWrite]s and a
 * [com.rossomak.flashcards.core.domain.model.ProgressSummaryWrite] carried on
 * [com.rossomak.flashcards.core.domain.model.SessionCommit] and applied by
 * [StudySessionRepository.commitSession] instead.
 */
interface CardProgressRepository {
    /** `null` when the User has never studied a card in this Subcategory yet. */
    suspend fun getProgress(subcategoryId: String): Result<SubcategoryProgress?>

    /** `null` when the User has never finished a session at all — spec 06 renders every ring empty in that case. */
    suspend fun getProgressSummary(): Result<ProgressSummary?>
}
