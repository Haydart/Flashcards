package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.source.VoiceSettingsLocalDataSource
import com.rossomak.flashcards.core.domain.model.VoiceSettings
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
class DefaultVoiceSettingsRepositoryTest {

    private val localDataSource: VoiceSettingsLocalDataSource = mockk()

    private fun createRepository(): DefaultVoiceSettingsRepository =
        DefaultVoiceSettingsRepository(localDataSource)

    @Test
    fun `voiceSettings delegates to the local data source`() = runTest {
        val settings = VoiceSettings(speechRate = 1.5f, voiceId = "en-us-x-1")
        every { localDataSource.voiceSettings() } returns flowOf(settings)

        val result = createRepository().voiceSettings().first()

        result shouldBe settings
    }

    @Test
    fun `save delegates to the local data source`() = runTest {
        val settings = VoiceSettings(speechRate = 1.25f, voiceId = null)
        coEvery { localDataSource.save(settings) } returns Unit

        createRepository().save(settings)

        coVerify(exactly = 1) { localDataSource.save(settings) }
    }
}
