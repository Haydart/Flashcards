package com.rossomak.flashcards.core.domain.model

/**
 * Everything one session's commit writes, bundled so [com.rossomak.flashcards.core.domain.repository.StudySessionRepository.commitSession]'s
 * signature can stay fixed while what it writes grows — ticket 03 and spec 05 add further fields
 * here rather than further parameters there. [newCardsStudied] and [progressWrites] are computed by
 * [com.rossomak.flashcards.core.domain.usecase.CommitStudySessionUseCase] from a read of prior
 * progress; [sessionResult] carries everything ticket 01 already writes, unchanged.
 */
data class SessionCommit(
    val sessionResult: SessionResult,
    val newCardsStudied: Int,
    val progressWrites: List<SubcategoryProgressWrite>,
)
