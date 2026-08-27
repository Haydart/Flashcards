package com.rossomak.flashcards.core.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.DefaultStudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.RatedAttempts
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.ReadAloudEnabled
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.SessionLength
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.SortOrder
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.VoiceAnsweringEnabled
import com.rossomak.flashcards.core.domain.model.StudySessionPreferences
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
class DataStoreStudySessionPreferencesLocalDataSourceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(scope = dataStoreScope) {
            temporaryFolder.newFile("user_preferences.preferences_pb")
        }
    }

    private fun createLocalDataSource(): DataStoreStudySessionPreferencesLocalDataSource =
        DataStoreStudySessionPreferencesLocalDataSource(dataStore)

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `studySessionPreferences emits defaults when nothing is persisted`() = runTest {
        val preferences = createLocalDataSource().studySessionPreferences().first()

        preferences shouldBe StudySessionPreferences()
    }

    @Test
    fun `save then read round-trips every field`() = runTest {
        val localDataSource = createLocalDataSource()

        localDataSource.save(DefaultStudyMode(StudyMode.Fast))
        localDataSource.save(VoiceAnsweringEnabled(true))
        localDataSource.save(RatedAttempts(2))
        localDataSource.save(ReadAloudEnabled(true))
        localDataSource.save(SessionLength(35))
        localDataSource.save(SortOrder(FlashcardSortOrder.HardestFirst))
        val preferences = localDataSource.studySessionPreferences().first()

        preferences shouldBe StudySessionPreferences(
            defaultStudyMode = StudyMode.Fast,
            voiceAnsweringEnabled = true,
            ratedAttempts = 2,
            readAloudEnabled = true,
            sessionLength = 35,
            sortOrder = FlashcardSortOrder.HardestFirst,
        )
    }

    @Test
    fun `an unknown persisted study mode falls back to the default`() = runTest {
        dataStore.edit { it[stringPreferencesKey("default_study_mode")] = "NoLongerExists" }

        val preferences = createLocalDataSource().studySessionPreferences().first()

        preferences.defaultStudyMode shouldBe StudySessionPreferences().defaultStudyMode
    }

    @Test
    fun `an unknown persisted sort order falls back to the default`() = runTest {
        dataStore.edit { it[stringPreferencesKey("sort_order")] = "NoLongerExists" }

        val preferences = createLocalDataSource().studySessionPreferences().first()

        preferences.sortOrder shouldBe FlashcardSortOrder.Default
    }

    @Test
    fun `sessionLength defaults to StudySessionConfig's default`() = runTest {
        val preferences = createLocalDataSource().studySessionPreferences().first()

        preferences.sessionLength shouldBe StudySessionConfig.DEFAULT_LENGTH
    }
}
