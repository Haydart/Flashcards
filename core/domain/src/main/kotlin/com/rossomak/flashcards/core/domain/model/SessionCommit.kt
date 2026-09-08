package com.rossomak.flashcards.core.domain.model

/**
 * Everything one session's commit writes, bundled so [com.rossomak.flashcards.core.domain.repository.StudySessionRepository.commitSession]'s
 * signature can stay fixed while what it writes grows — spec 05 adds a further field here rather
 * than a further parameter there. [newCardsStudied], [progressWrites] and [progressSummaryWrite] are
 * all computed by [com.rossomak.flashcards.core.domain.usecase.CommitStudySessionUseCase] from the
 * same read of prior progress; [sessionResult] carries everything ticket 01 already writes,
 * unchanged.
 *
 * [xpBreakdown] and [newScoringState] are spec 05 ticket 02's addition:
 * [com.rossomak.flashcards.core.domain.usecase.CalculateSessionXpUseCase]'s output, computed from
 * [sessionResult], [newCardsStudied] and a read of the account's prior [ScoringState]. [xpBreakdown]
 * joins the `sessions/{sessionId}` document itself, alongside everything [sessionResult] already
 * writes there; [newScoringState] becomes the whole `progress/user-stats` document — one further
 * write in the same batch, not a restructure of this type's shape.
 */
data class SessionCommit(
    val sessionResult: SessionResult,
    val newCardsStudied: Int,
    val progressWrites: List<SubcategoryProgressWrite>,
    val progressSummaryWrite: ProgressSummaryWrite,
    val xpBreakdown: XpBreakdown,
    val newScoringState: ScoringState,
)
