package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.repository.FakeFlashcardRepository
import com.rossomak.flashcards.core.domain.repository.FakeUserPreferencesRepository
import io.kotest.matchers.shouldBe
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncFlashcardCacheGenerationUseCaseTest {

    private val flashcardRepository = FakeFlashcardRepository()
    private val userPreferencesRepository = FakeUserPreferencesRepository()

    private val syncFlashcardCacheGeneration = SyncFlashcardCacheGenerationUseCase(
        flashcardRepository = flashcardRepository,
        userPreferencesRepository = userPreferencesRepository,
        saveUserPreference = SaveUserPreferenceUseCase(userPreferencesRepository),
    )

    @Test
    fun `a mismatch invalidates the cache and persists the new seed`() = runTest {
        userPreferencesRepository.preferences.value = userPreferencesRepository.preferences.value.copy(localCacheSeed = 3)
        flashcardRepository.cacheSeedToReturn = Result.success(4)

        syncFlashcardCacheGeneration()

        flashcardRepository.invalidationCount shouldBe 1
        userPreferencesRepository.preferences.value.localCacheSeed shouldBe 4
    }

    @Test
    fun `never-checked local seed mismatches unconditionally`() = runTest {
        userPreferencesRepository.preferences.value = userPreferencesRepository.preferences.value.copy(localCacheSeed = null)
        flashcardRepository.cacheSeedToReturn = Result.success(1)

        syncFlashcardCacheGeneration()

        flashcardRepository.invalidationCount shouldBe 1
        userPreferencesRepository.preferences.value.localCacheSeed shouldBe 1
    }

    @Test
    fun `a match is a no-op`() = runTest {
        userPreferencesRepository.preferences.value = userPreferencesRepository.preferences.value.copy(localCacheSeed = 5)
        flashcardRepository.cacheSeedToReturn = Result.success(5)

        syncFlashcardCacheGeneration()

        flashcardRepository.invalidationCount shouldBe 0
    }

    @Test
    fun `a remote read failure is swallowed silently`() = runTest {
        userPreferencesRepository.preferences.value = userPreferencesRepository.preferences.value.copy(localCacheSeed = 3)
        flashcardRepository.cacheSeedToReturn = Result.failure(IOException("offline"))

        syncFlashcardCacheGeneration()

        flashcardRepository.invalidationCount shouldBe 0
        userPreferencesRepository.preferences.value.localCacheSeed shouldBe 3
    }

    @Test
    fun `a persist failure after invalidating is swallowed silently, not thrown`() = runTest {
        userPreferencesRepository.preferences.value = userPreferencesRepository.preferences.value.copy(localCacheSeed = 3)
        flashcardRepository.cacheSeedToReturn = Result.success(4)
        userPreferencesRepository.saveError = IOException("disk full")

        syncFlashcardCacheGeneration()

        flashcardRepository.invalidationCount shouldBe 1
        userPreferencesRepository.preferences.value.localCacheSeed shouldBe 3
    }
}
