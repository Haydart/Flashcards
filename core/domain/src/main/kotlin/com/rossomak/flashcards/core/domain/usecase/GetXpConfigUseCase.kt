package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.XpConfig
import com.rossomak.flashcards.core.domain.repository.XpConfigRepository
import com.rossomak.flashcards.core.domain.usecase.base.NoParamUseCase
import javax.inject.Inject

/**
 * The one place a fetch failure is turned into [XpConfig]'s defaults rather than an error — a
 * Study Session must always be able to start, so a configuration read that fails costs at most a
 * tuning adjustment, never the session itself (ADR-0047). Every caller gets a plain, never-failing
 * [XpConfig] back; nothing downstream needs to know whether the value came from the repository or
 * the fallback.
 */
class GetXpConfigUseCase @Inject constructor(
    private val xpConfigRepository: XpConfigRepository,
) : NoParamUseCase<XpConfig> {

    override suspend operator fun invoke(): XpConfig = xpConfigRepository.getXpConfig().getOrDefault(XpConfig())
}
