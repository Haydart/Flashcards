package com.rossomak.flashcards.core.ui.theme

import com.caverock.androidsvg.SVG

/**
 * Parses [Category.iconSvg][com.rossomak.flashcards.core.domain.model.Category.iconSvg] — a plain
 * SVG document, not Android VectorDrawable XML — via `androidsvg` rather than a first-party
 * parser: real category icons carry `transform="translate(...) scale(...)"` attributes, a
 * compound mini-language a hand-rolled parser would have to tokenize itself, and `androidsvg`
 * implements full SVG semantics (transforms, nested groups, fill rules) for free. See
 * docs/design/category-icon-color.md's "Icon format" section for the tradeoff against the
 * first-party VectorDrawable-XML approach this superseded.
 *
 * `fill`/`fill-rule` in the source SVG are irrelevant and never read: tint always overrides the
 * rendered color at draw time (see `FlashcardsVectorIconTile`), so a genuinely multi-color source
 * icon silently flattens to one tint color.
 *
 * Hand-curated, authored data (not user input), so malformed SVG throws — callers that need a
 * graceful fallback wrap this in `runCatching { }.getOrNull()` (see `FlashcardsVectorIconTile`).
 *
 * Not JVM-unit-tested: rendering the parsed [SVG] to a `Picture` requires `android.graphics.
 * Picture`, an Android-framework class stubbed on this project's plain-JVM, no-Robolectric test
 * stack (see TESTING.md) — same boundary the previous VectorDrawable-XML parser hit, just moved
 * from parse-time to render-time. Correctness is verified by compiling + a Compose
 * preview/Showkase visual check, same as every other rendering composable in this app.
 */
fun String.toCategorySvg(): SVG = SVG.getFromString(this)
