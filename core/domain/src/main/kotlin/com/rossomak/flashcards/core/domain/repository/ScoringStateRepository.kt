package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.ScoringState

/**
 * Reads the User's account-wide [ScoringState] singleton, `progress/user-stats` (spec 05 ticket 02).
 * Writing is not exposed here: the updated state must land in the same Firestore batch as the rest of
 * a session's commit, so [com.rossomak.flashcards.core.domain.repository.StudySessionRepository.commitSession]
 * is the only write path, same shape as [com.rossomak.flashcards.core.domain.model.ProgressSummaryWrite].
 *
 * @return `Result.success(null)` for an account with no scoring state document yet — a genuinely new
 * user, not a failure — leaving it to the caller to start from [ScoringState]'s own defaults.
 * `Result.failure` for a real read failure, which
 * [com.rossomak.flashcards.core.domain.usecase.CommitStudySessionUseCase] must never paper over with
 * a default: guessing a low starting state and writing it back would silently overwrite a real
 * account's accumulated XP.
 */
interface ScoringStateRepository {

    suspend fun getScoringState(): Result<ScoringState?>
}
