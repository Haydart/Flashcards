package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.ScoringState

/**
 * Reads the User's account-wide [ScoringState] singleton, `progress/user-stats`.
 * Writing is not exposed here, nor anywhere else on the client: the server-authoritative
 * `submitStudySession` Cloud Function is the sole writer of this document, computing and
 * overwriting the whole next [ScoringState] itself inside its own Firestore transaction.
 * [getScoringState] only ever feeds
 * [com.rossomak.flashcards.core.domain.usecase.SubmitStudySessionUseCase]'s optimistic preview now.
 *
 * @return `Result.success(null)` for an account with no scoring state document yet — a genuinely new
 * user, not a failure — leaving it to the caller to start from [ScoringState]'s own defaults.
 * `Result.failure` for a real read failure, which
 * [com.rossomak.flashcards.core.domain.usecase.SubmitStudySessionUseCase] must never paper over with
 * a default for its preview: guessing a low starting state would show a misleadingly small number,
 * even though — unlike the old client-write path — it can no longer corrupt any persisted state.
 */
interface ScoringStateRepository {

    suspend fun getScoringState(): Result<ScoringState?>
}
