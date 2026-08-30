package com.rossomak.flashcards.core.data.source

/**
 * Where a flashcard read is allowed to be served from.
 *
 * Exists so the repository can express a cache-first policy without importing Firestore's own
 * `Source` — the mapping to it stays inside [FlashcardRemoteDataSource], which is the only place
 * that knows the backend at all.
 */
enum class FlashcardReadSource {

    /**
     * On-device cache only, never the network. Returns an **empty list** on a miss rather than
     * failing, which is why the caller needs the never-empty Subcategory invariant to read that
     * emptiness as "not cached" (ADR-0038).
     */
    Cache,

    /** The backend, bypassing the cache. */
    Server,
}
