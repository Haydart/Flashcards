package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.XpConfig

/**
 * Serves the current [XpConfig]. The local implementation shipped in this ticket never actually
 * fails, but the seam returns [Result] anyway: it is what lets a future remote source fail without
 * a signature change, and [com.rossomak.flashcards.core.domain.usecase.GetXpConfigUseCase] already
 * falls back to [XpConfig]'s defaults on failure — a session must always be able to start.
 */
interface XpConfigRepository {
    suspend fun getXpConfig(): Result<XpConfig>
}
