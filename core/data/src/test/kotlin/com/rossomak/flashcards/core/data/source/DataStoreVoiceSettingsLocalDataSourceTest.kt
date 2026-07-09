package com.rossomak.flashcards.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultVoiceSettingsRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(scope = dataStoreScope) {
            temporaryFolder.newFile("voice_settings.preferences_pb")
        }
    }

    private fun createRepository(): DefaultVoiceSettingsRepository =
        DefaultVoiceSettingsRepository(dataStore)

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `voiceSettings emits defaults when nothing is persisted`() = runTest {
        val settings = createRepository().voiceSettings().first()

        settings shouldBe VoiceSettings()
    }

    @Test
    fun `save then read round-trips speech rate and voice id`() = runTest {
        val repository = createRepository()
        val persisted = VoiceSettings(speechRate = 1.5f, voiceId = "en-us-x-1")

        repository.save(persisted)
        val settings = repository.voiceSettings().first()

        settings shouldBe persisted
    }

    @Test
    fun `save with null voice id clears a previously stored voice id`() = runTest {
        val repository = createRepository()
        val speechRate = 1.25f
        repository.save(VoiceSettings(speechRate = speechRate, voiceId = "en-us-x-1"))

        repository.save(VoiceSettings(speechRate = speechRate, voiceId = null))
        val settings = repository.voiceSettings().first()

        settings.voiceId shouldBe null
        settings.speechRate shouldBe speechRate
    }
}
