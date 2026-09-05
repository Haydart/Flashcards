package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.source.VoiceOptionsDataSource
import com.rossomak.flashcards.core.domain.model.VoiceOption
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultVoiceOptionsRepositoryTest {

    private val dataSource: VoiceOptionsDataSource = mockk()

    private fun createRepository(): DefaultVoiceOptionsRepository =
        DefaultVoiceOptionsRepository(dataSource)

    @Test
    fun `getAvailableVoices returns voices from data source unchanged`() = runTest {
        val voices = listOf(
            VoiceOption(id = "en-us-x-1", countryCode = "US", variantIndex = 1),
            VoiceOption(id = "en-gb-x-2", countryCode = "GB", variantIndex = 1),
        )
        coEvery { dataSource.getAvailableVoices() } returns voices

        val result = createRepository().getAvailableVoices()

        result shouldBe voices
        coVerify(exactly = 1) { dataSource.getAvailableVoices() }
    }

    @Test
    fun `getAvailableVoices returns empty list when data source has no voices`() = runTest {
        coEvery { dataSource.getAvailableVoices() } returns emptyList()

        val result = createRepository().getAvailableVoices()

        result shouldBe emptyList()
        coVerify(exactly = 1) { dataSource.getAvailableVoices() }
    }
}
