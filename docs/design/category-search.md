# Category & Subcategory Search

**Status:** Design only — not yet implemented. Scoped as its own feature branch: touches Firestore schema (ADR-0007), the seed pipeline, `core:data`, `core:domain`, and the Study tab UI.

## Overview

The Study tab search box (search categories/topics) needs to feel "smart" — searching "compose" should surface both the `Compose` Subcategory and the `Android` Category (because one of its children matched); searching "testing" should surface every matching Subcategory across every Category, plus each of those parent Categories. All of this must run on a debounced live Firestore query, never a full local cache of `subcategories` (that collection is expected to keep growing; loading it in full to answer a "handful of subcategories" search is wasteful).

This doc covers matching rules, the two Firestore schema fields it depends on, the read-cost budget for both the default Browse state and live search, and where the logic lives in the module structure. Related: [Category icon+color](category-icon-color.md) (sibling denormalized-field design), [Persistent Card Mastery](persistent-card-mastery.md) (the `subcategoryProgress` rollup that supplies the progress rings shown alongside matched topics).

## Matching rule: prefix-only, uniform

Firestore has no substring/"contains" query — only range queries, which give **prefix matching** (`field >= "compose" AND field < "compose"`). Both worked examples in this design ("compose", "testing") happen to be literal prefixes of the Subcategory names they match, so this is not a regression for the motivating cases, but it is a real narrowing from arbitrary substring matching (e.g. typing "avigation" will not match "Navigation").

Category-name matching happens entirely client-side against an always-cached list (see below) and has no such technical constraint — it *could* do true substring matching for free. It doesn't: **prefix-only is applied uniformly** to both categories and subcategories, so the search box has one matching rule end to end rather than a rule that quietly differs depending on what's being matched.

Case-insensitivity: comparisons are done against a lowercased field (`nameLower` — see below), since Firestore range comparisons are case-sensitive.

Out of scope for v1: synonym/fuzzy matching (e.g. "async" → Coroutines). Additive later via a `searchTerms: List<String>` field on Subcategory if needed — doesn't conflict with this design.

## Firestore schema additions (ADR-0007)

```
categories/{categoryId}     → { ..., topSubcategoryNames: [String] }   (up to 5, see below)
subcategories/{subcategoryId} → { ..., nameLower: String }
```

`topSubcategoryNames`: the display names of the top-5 Subcategories per Category, ranked by the same card-volume-descending order that already produces each Subcategory's `order` field (`build_fixture.py`). Computed once at fixture-build time. **Not sticky** — unlike `iconSvg`/`color`, this is pure derived data with no manual curation, so every reseed overwrites it unconditionally as ranks shift with card counts.

`nameLower`: `name.lower()`, computed at fixture-build time for every Subcategory. Exists solely to make prefix-range queries case-insensitive.

## Read budget

Two collections, two very different caching strategies, because of their size trajectory: `categories` is small and fixed (4 today); `subcategories` is larger and grows over time (84 today).

**`categories`** — always fully cached client-side (one listener, established once when the Study tab is first opened). Cost: **N reads**, N = category count, once per cold session. Category-name matching and the default-state topic chips both read from this cache — zero further Firestore cost.

**`subcategories`** — never bulk-loaded. Only touched by a live, debounced query scoped to whatever the user is currently typing:

```
subcategories
    .whereGreaterThanOrEqualTo("nameLower", query)
    .whereLessThan("nameLower", query + "")
```

Cost: exactly the number of matched Subcategory docs — "a handful," not the full collection. Each returned doc already carries `categoryId`/`categoryName` (existing denormalized fields, ADR-0007), so grouping a match into its parent Category needs no further read; the parent's chip-fill data comes from the already-cached `categories` list.

**Browse screen entry, end to end: exactly N reads** (one query on `categories`). `subcategories` is untouched until the user interacts with search.

## Live search behavior

- **Debounce:** 500ms after the last keystroke before firing a query.
- **Query cache:** exact-string keyed (`"testing" → [matched docs]`), in-memory, scoped to the search ViewModel/use case lifetime — cleared on leaving the screen or process death. Not persisted. Not prefix-aware (a query for `"testin"` after already having fetched `"testing"` results is a fresh live query, not a local filter of the cached superset) — deliberately the simplest option that satisfies "don't repeat a query we already ran"; a prefix-refinement cache (exploiting that `"testin"` results ⊇ `"testing"` results) is a viable future optimization if read volume warrants it.

## Chip-line construction

Both the default Browse state and live search results render a short list of Subcategory names under each Category row (`Compose · Coroutines · Compose Navig…`, truncated by available width in the UI, not by a fixed count — the underlying data always carries up to 5 names).

**Default state:** `Category.topSubcategoryNames` directly, in stored order (card-volume descending).

**Search-mode (query active):** matched Subcategory(ies) for that Category come first, then the list is backfilled — skipping duplicates — from `topSubcategoryNames`/prominence order until 5 names are reached. If more than one Subcategory within the same Category matches the query, the matches themselves are ordered by their own prominence `order` (not match position or alphabetically) — one ranking signal used everywhere in this design, no special case for the multi-match branch.

Example: query "testing" → Android chip line is `Testing, Compose, Background` (Testing is the match; Compose/Background are Android's next-most-prominent topics, unrelated to the query, filling out the line).

## Layering

A `SearchCategoriesUseCase` in `core:domain` owns the whole pipeline: local category-name matching, the debounced live Subcategory query, the exact-string query cache, and chip-fill ranking. The Study tab ViewModel owns only the debounced text-input `Flow` and calls the use case — matches the existing `GetFlashcardsUseCase` pattern rather than putting query/cache/ranking logic directly in the ViewModel.

## Progress rings on matched topics

Search results show a mastery-percentage ring next to each matched topic (see screenshot in Study tab designs). This reuses the `subcategoryProgress` rollup counter from [Persistent Card Mastery](persistent-card-mastery.md) unchanged: `where subcategoryId in [matchedIds]` (Firestore `in` caps at 30) against `users/{uid}/subcategoryProgress`, scoped to exactly the Subcategories the search already matched — no new read pattern needed, it composes directly with the live-query result set this design already produces.

## Explicitly deferred (not v1)

- Synonym/fuzzy matching (`searchTerms` field).
- Prefix-refinement query cache (serving a longer-prefix query by filtering a cached shorter-prefix superset locally).
- Persisting the query cache across sessions.
