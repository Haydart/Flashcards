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
 * A match is a true no-op — nothing to do. A failure reading the remote seed is a no-op too, in
 * the same sense: [FlashcardRepository.fetchCacheSeed] returns a [Result] rather than throwing,
 * and a failure short-circuits before anything happens.
 *
 * A failure *persisting* the new seed is not a no-op, even though it's also silently discarded: it
 * happens after [invalidateFlashcardCache][FlashcardRepository.invalidateFlashcardCache] has
 * already run, so the cache generation genuinely bumped — only the local "what seed did we last
 * see" bookkeeping failed to update. That's a partial success: [saveUserPreference] swallows the
 * persist failure into a [Result] this use case discards, so the mismatch simply reappears and
 * retries on the next launch rather than being treated as an error here.
 *
 * A local read failure while fetching the *current* local seed reads as `null` too —
 * `DataStoreUserPreferencesLocalDataSource` maps that read's `IOException` to empty preferences,
 * indistinguishable from "never checked." Accepted: it costs one avoidable cache refresh, never
 * wrong data, and self-corrects the moment the local read succeeds again.
 *
 * Best-effort by design throughout: a stale cache is recoverable next launch, a crashed app start
 * is not.
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
