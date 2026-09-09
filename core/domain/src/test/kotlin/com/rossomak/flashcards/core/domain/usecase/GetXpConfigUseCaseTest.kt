package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.XpConfig
import com.rossomak.flashcards.core.domain.repository.FakeXpConfigRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The one place a failed [com.rossomak.flashcards.core.domain.repository.XpConfigRepository] fetch
 * is turned into [XpConfig]'s defaults (ADR-0047) — a Study Session must always be able to start.
 */
class GetXpConfigUseCaseTest {

    private val xpConfigRepository = FakeXpConfigRepository()
    private val useCase = GetXpConfigUseCase(xpConfigRepository)

    @Test
    fun `returns the repository's config on success`() = runTest {
        val config = XpConfig(newCardStudied = 999)
        xpConfigRepository.resultToReturn = Result.success(config)

        useCase() shouldBe config
    }

    @Test
    fun `falls back to defaults when the repository fetch fails`() = runTest {
        xpConfigRepository.resultToReturn = Result.failure(IllegalStateException("offline"))

        useCase() shouldBe XpConfig()
    }
}
