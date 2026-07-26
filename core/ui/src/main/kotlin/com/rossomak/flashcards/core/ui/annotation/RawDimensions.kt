package com.rossomak.flashcards.core.ui.annotation

/**
 * Opt-out marker for the design-system "no raw dp literals" rule (see `core/ui/README.md`).
 *
 * Apply to a composable in the `composables/` package that legitimately needs one-off
 * geometry with no semantic theme token. The [reason] is mandatory and is surfaced by
 * `grep -r RawDimensions` when auditing escapes. Colors are never exempt — always use
 * `colorScheme` / `brandColors`.
 *
 * The Konsist rule `design-system composables use no raw dp literals` skips any function
 * annotated with this.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class RawDimensions(val reason: String)
