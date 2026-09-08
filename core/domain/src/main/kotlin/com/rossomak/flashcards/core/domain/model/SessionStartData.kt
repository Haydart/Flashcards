package com.rossomak.flashcards.core.domain.model

/**
 * What a Study Session ViewModel needs at load time, bundled from
 * [com.rossomak.flashcards.core.domain.usecase.GetSessionStartDataUseCase]'s parallel fan-out over
 * the routed Subcategories (ticket 04 of spec 04 session persistence), plus the XP configuration
 * snapshot (ticket 01 of spec 05).
 *
 * The two card-progress-adjacent halves degrade independently, on purpose: [flashcardsResult] fails
 * the whole session (no cards, nothing to study), while [priorProgressByCardId] never does — a
 * failed or never-studied Subcategory's progress read simply contributes no entries, same as a
 * genuinely absent one, rather than blocking the session or surfacing an error. [xpConfig] follows
 * the same never-blocks philosophy by construction: [com.rossomak.flashcards.core.domain.usecase.GetXpConfigUseCase]
 * already falls back to [XpConfig]'s defaults on a failed fetch, so this is always a plain value,
 * never a [Result].
 */
data class SessionStartData(
    val flashcardsResult: Result<List<Flashcard>>,
    /** cardId -> its packed progress entry, merged across every Subcategory in scope; a missing key means new. */
    val priorProgressByCardId: Map<String, CardProgressEntry>,
    /**
     * The scoring configuration as of this session's start — carried into the session state and
     * forward onto [SessionResult.xpConfig] rather than re-read later (ADR-0047's snapshot rule).
     */
    val xpConfig: XpConfig,
)
