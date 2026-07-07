package com.rossomak.flashcards.domain.usecase

import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import com.rossomak.flashcards.core.domain.usecase.CheckVoiceGradingEntitlementUseCase
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CheckVoiceGradingEntitlementUseCaseTest {

    private val voiceAnswerGradingRepository: VoiceAnswerGradingRepository = mockk()
    private val useCase = CheckVoiceGradingEntitlementUseCase(voiceAnswerGradingRepository)

    @Test
    fun `returns the entitlement verdict from the repository`() = runTest {
        coEvery { voiceAnswerGradingRepository.checkEntitlement() } returns Result.success(true)

        val result = useCase()

        result.getOrNull() shouldBe true
        coVerify(exactly = 1) { voiceAnswerGradingRepository.checkEntitlement() }
    }

    @Test
    fun `forwards failure result unchanged`() = runTest {
        val error = IllegalStateException("backend down")
        coEvery { voiceAnswerGradingRepository.checkEntitlement() } returns Result.failure(error)

        val result = useCase()

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { voiceAnswerGradingRepository.checkEntitlement() }
    }
}
