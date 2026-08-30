package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.UserPreference.CacheSeed
import com.rossomak.flashcards.core.domain.repository.FlashcardRepository
import com.rossomak.flashcards.core.domain.repository.UserPreferencesRepository
import com.rossomak.flashcards.core.domain.usecase.base.NoParamUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Seed-versioned flashcard cache invalidation (ADR-0039): compares the server's `meta/seed`
 * against the locally stored copy and, on a mismatch, starts a new flashcard cache generation
 * before persisting the new value.
 *
 * A match is a no-op. So is any failure along the way: [FlashcardRepository.fetchCacheSeed]
 * already returns a [Result] rather than throwing, and [saveUserPreference] already swallows a
 * persist failure into a [Result] of its own (its generic catch, not repeated here) — this use
 * case discards both outcomes rather than surfacing them. Best-effort by design: a stale cache is
 * recoverable next launch, a crashed app start is not.
 */
class SyncFlashcardCacheGenerationUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val saveUserPreference: SaveUserPreferenceUseCase,
) : NoParamUseCase<Unit> {

    override suspend operator fun invoke() {
        val remoteSeed = flashcardRepository.fetchCacheSeed().getOrNull() ?: return
        val localSeed = userPreferencesRepository.userPreferences().first().localCacheSeed

        if (remoteSeed == localSeed) return

        flashcardRepository.invalidateFlashcardCache()
        saveUserPreference(CacheSeed(remoteSeed))
    }
}
