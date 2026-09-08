package com.rossomak.flashcards.core.domain.model

/**
 * Every tunable scoring number spec 05 (XP and leveling) needs, in one place — no point award, no
 * penalty, and no level-curve parameter is ever a constant in domain logic; the calculation that
 * eventually consumes this reads every value from here instead
 * ([ADR-0047](../../../../../../../docs/adr/0047-xp-values-behind-a-config-repository.md)).
 *
 * Served by [com.rossomak.flashcards.core.domain.repository.XpConfigRepository]. The default
 * constructor values *are* the documented defaults — the local repository implementation returns a
 * plain `XpConfig()`, and this ticket ships no other source. A future remote source would need a
 * cache and a defaulting story, since scoring must work offline; these defaults are that fallback
 * too, via [com.rossomak.flashcards.core.domain.usecase.GetXpConfigUseCase].
 *
 * **A session is scored against the [XpConfig] captured when it started.** It is fetched alongside
 * the session's cards, carried in the session state, and carried forward on [SessionResult.xpConfig]
 * — the Summary screen computes from that snapshot, never from a fresh read, so a value changing
 * mid-session cannot rewrite the arithmetic for a session already in progress.
 *
 * @param newCardStudied per card seeing a card for the first time ever. Both Study Modes.
 * @param cardMastered per card ending Mastered. Rated only — Fast has no mastery concept.
 * @param cardPartial per card ending Partial. Rated only.
 * @param masteryDefended per card that keeps a previously-mastered card Mastered again. Rated only,
 * and structurally unreachable until spec 07's Mastery Defense selection exists.
 * @param cardDemastered per card that loses a previously-mastered card's mastery — negative. Rated
 * only, same spec 07 dependency as [masteryDefended].
 * @param sessionCompleted flat, once, only for a session that finishes its deck rather than being
 * abandoned. Both modes.
 * @param dailyGoalMet flat, once per calendar day the daily study-minutes goal is met. Both modes.
 * @param streakPerDay per consecutive study day, before [streakMaxPerDay] caps it. Both modes.
 * @param streakMaxPerDay the ceiling [streakPerDay] × streak-length is clamped to.
 * @param minuteStudied per minute of session time. Both modes.
 * @param levelCurveBase the level curve's `base` in `ceil(base × level^exponent / 1000) × 1000` —
 * a tuning value, not yet chosen for real (spec 05's stated shape: early levels reachable in one or
 * two good sessions, the middle range demanding multi-day effort, the high levels long-term).
 * @param levelCurveExponent the curve's `exponent`, same formula, same tuning status as
 * [levelCurveBase].
 */
data class XpConfig(
    val newCardStudied: Int = 10,
    val cardMastered: Int = 100,
    val cardPartial: Int = 25,
    val masteryDefended: Int = 50,
    val cardDemastered: Int = -CARD_DEMASTERED_MAGNITUDE,
    val sessionCompleted: Int = 500,
    val dailyGoalMet: Int = 1000,
    val streakPerDay: Int = 250,
    val streakMaxPerDay: Int = 2500,
    val minuteStudied: Int = 10,
    val levelCurveBase: Double = 1000.0,
    val levelCurveExponent: Double = 2.5,
) {
    private companion object {
        // Named rather than inlined as a bare -80 default above — detekt's MagicNumber check does
        // not see through the unary minus on a raw literal default value the way it does a plain one.
        const val CARD_DEMASTERED_MAGNITUDE = 80
    }
}
