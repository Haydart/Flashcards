package com.rossomak.flashcards.di

import com.rossomak.flashcards.core.domain.model.StudyPreferences
import com.rossomak.flashcards.core.domain.model.UserPreferences
import com.rossomak.flashcards.core.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Temporary [UserPreferencesRepository] binding: holds state in memory only, for the lifetime of
 * the process, so [com.rossomak.flashcards.feature.onboarding.OnboardingViewModel],
 * [com.rossomak.flashcards.feature.settings.SettingsViewModel] and
 * [com.rossomak.flashcards.presentation.splash.SplashViewModel] have a real, injectable
 * implementation to build and run against before the DataStore-backed one lands.
 *
 * **Not the persistence-writing implementation, and deliberately not in `:core:data`.** Nothing
 * here survives a process death or app restart — `hasSeenOnboarding` resets to `false` every
 * launch, so onboarding always replays. The real implementation (DataStore-backed
 * `UserPreferencesLocalDataSource` / `UserPreferencesRepository` / Hilt module) is written on this
 * branch but held out of this commit on purpose — it lands in a follow-up PR, which should delete
 * this whole file.
 *
 * Lives in `:app` rather than `:core:data` for exactly that reason: it is scaffolding for this app
 * target only, not a `:core:data` implementation other modules should be able to depend on.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TemporaryUserPreferencesModule {

    @Binds
    @Singleton
    internal abstract fun bindUserPreferencesRepository(
        impl: InMemoryUserPreferencesRepository,
    ): UserPreferencesRepository
}

internal class InMemoryUserPreferencesRepository @Inject constructor() : UserPreferencesRepository {

    private val preferences = MutableStateFlow(UserPreferences())

    override fun userPreferences(): Flow<UserPreferences> = preferences

    override suspend fun saveStudyPreferences(studyPreferences: StudyPreferences) {
        preferences.value = preferences.value.copy(studyPreferences = studyPreferences)
    }

    override suspend fun setHasSeenOnboarding(hasSeenOnboarding: Boolean) {
        preferences.value = preferences.value.copy(hasSeenOnboarding = hasSeenOnboarding)
    }
}
