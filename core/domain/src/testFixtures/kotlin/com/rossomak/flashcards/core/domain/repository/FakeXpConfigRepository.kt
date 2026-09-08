package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.XpConfig

class FakeXpConfigRepository : XpConfigRepository {
    /** Overrides every [getXpConfig] call, success or failure alike. */
    var resultToReturn: Result<XpConfig> = Result.success(XpConfig())

    override suspend fun getXpConfig(): Result<XpConfig> = resultToReturn
}
