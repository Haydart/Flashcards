package com.rossomak.flashcards.core.domain.model

/**
 * The cards a [StudySessionConfig] resolves to, how long working through them is expected to take,
 * and the tag vocabulary of the pool they were drawn from.
 *
 * A plain data holder — it owns no arithmetic. [estimatedMinutes] is computed by whichever use case
 * builds this plan ([com.rossomak.flashcards.core.domain.usecase.SelectSessionFlashcardsUseCase]
 * today), so swapping a local estimate for one a future backend/AI selector returns touches only
 * that use case, never this model or its consumers.
 */
data class StudySessionPlan(
    val cards: List<Flashcard>,
    val estimatedMinutes: Int,
    val poolTags: List<String>,
)
