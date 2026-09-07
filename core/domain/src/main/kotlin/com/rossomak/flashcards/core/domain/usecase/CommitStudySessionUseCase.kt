package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.repository.StudySessionRepository
import javax.inject.Inject

/**
 * The single use case that performs the whole session save (ADR-0014), called once by the Summary
 * ViewModel on arrival. A thin pass-through today — [StudySessionRepository.commitSession] already
 * carries the whole [SessionResult] and the single Firestore batch it becomes is entirely the
 * repository's concern, so there is nothing for this seam to add yet. Tickets 02/03 grow what the
 * batch writes, not this use case's shape.
 *
 * Not a [com.rossomak.flashcards.core.domain.usecase.base.UseCase]: [onRejected] is a side channel
 * for an async, after-the-fact failure report (see [StudySessionRepository.commitSession]), not a
 * second input the single-param `UseCase<P, R>` shape is meant to carry.
 */
class CommitStudySessionUseCase @Inject constructor(
    private val studySessionRepository: StudySessionRepository,
) {

    suspend operator fun invoke(sessionResult: SessionResult, onRejected: (Throwable) -> Unit = {}): Result<Unit> =
        studySessionRepository.commitSession(sessionResult, onRejected)
}
