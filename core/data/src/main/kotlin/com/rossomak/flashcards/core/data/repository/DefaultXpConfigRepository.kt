package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.domain.model.XpConfig
import com.rossomak.flashcards.core.domain.repository.XpConfigRepository
import javax.inject.Inject

/**
 * Local and hardcoded: the documented defaults, in code, always succeeding. The seam exists so
 * balance can move to a remote source later without touching a call site (ADR-0047); this is the
 * offline default that source would still need.
 */
class DefaultXpConfigRepository @Inject constructor() : XpConfigRepository {

    override suspend fun getXpConfig(): Result<XpConfig> = Result.success(XpConfig())
}
