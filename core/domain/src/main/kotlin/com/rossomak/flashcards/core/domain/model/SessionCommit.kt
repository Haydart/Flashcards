package com.rossomak.flashcards.core.domain.model

/**
 * Everything one session's commit writes, bundled so [com.rossomak.flashcards.core.domain.repository.StudySessionRepository.commitSession]'s
 * signature can stay fixed while what it writes grows — spec 05 adds a further field here rather
 * than a further parameter there. [newCardsStudied], [progressWrites] and [progressSummaryWrite] are
 * all computed by [com.rossomak.flashcards.core.domain.usecase.CommitStudySessionUseCase] from the
 * same read of prior progress; [sessionResult] carries everything ticket 01 already writes,
 * unchanged.
 */
data class SessionCommit(
    val sessionResult: SessionResult,
    val newCardsStudied: Int,
    val progressWrites: List<SubcategoryProgressWrite>,
    val progressSummaryWrite: ProgressSummaryWrite,
)
