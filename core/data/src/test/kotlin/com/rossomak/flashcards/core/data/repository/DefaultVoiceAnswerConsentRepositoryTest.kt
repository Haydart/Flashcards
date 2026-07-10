package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.source.VoiceAnswerConsentLocalDataSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultVoiceAnswerConsentRepositoryTest {

    private val localDataSource: VoiceAnswerConsentLocalDataSource = mockk()

    private fun createRepository(): DefaultVoiceAnswerConsentRepository =
        DefaultVoiceAnswerConsentRepository(localDataSource)

    @Test
    fun `observeConsent delegates to the local data source`() = runTest {
        every { localDataSource.observeConsent() } returns flowOf(true)

        val result = createRepository().observeConsent().first()

        result shouldBe true
    }

    @Test
    fun `setConsent delegates to the local data source`() = runTest {
        coEvery { localDataSource.setConsent(true) } returns Unit

        createRepository().setConsent(true)

        coVerify(exactly = 1) { localDataSource.setConsent(true) }
    }
}
