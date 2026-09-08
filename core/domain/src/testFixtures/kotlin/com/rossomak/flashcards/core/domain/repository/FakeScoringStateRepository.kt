package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.ScoringState

class FakeScoringStateRepository : ScoringStateRepository {

    /** `null` (the default) simulates a brand-new account with no `progress/user-stats` document yet. */
    var resultToReturn: Result<ScoringState?> = Result.success(null)

    override suspend fun getScoringState(): Result<ScoringState?> = resultToReturn
}
