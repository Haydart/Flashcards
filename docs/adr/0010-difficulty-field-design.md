# Difficulty field: mandatory on global pool, filtered at DTO layer, immutable

> **Note:** The "Private Flashcards are exempt" decision below is superseded by [ADR-0022](0022-subcategory-details-filter-sort-toolbar.md) — difficulty is now mandatory on Private Flashcards too. The rest of this ADR (global-pool DTO filtering, immutability) still stands.

## Decision

Every global admin-curated Flashcard carries a mandatory `difficulty: Int` (1–10). Flashcard documents in Firestore that have no `difficulty` field are silently filtered out at the DTO→domain mapping layer and never reach the domain. Private Flashcards are exempt — they carry no Difficulty. Difficulty is immutable once written to Firestore.

## Context

Curriculum features require Flashcards to be ordered by complexity within a Subcategory. A per-card integer on a 1–10 domain-relative scale was chosen over categorical tiers (beginner/intermediate/advanced) to allow fine-grained ordering.

The field is being added retroactively — the initial card corpus predates it. During the backfill period, Firestore will contain a mix of scored and unscored documents.

Private Flashcards are user-created and follow a separate lifecycle (private → submitted → approved). Requiring users to self-assign difficulty at creation time introduces friction and produces inconsistent scores without a calibration baseline.

## Alternatives considered

**Nullable `difficulty: Int?` in the domain model** — rejected. Null propagates into curriculum logic as an edge case every caller must handle. Filtering at the DTO boundary is cleaner: the domain never sees an unscored card, and callers make no special case for null.

**Default sentinel value (e.g. 0)** — rejected. 0 is outside the valid 1–10 range and would silently pollute curriculum ordering if not stripped everywhere. A filter is honest; a sentinel is a lie.

**Difficulty on Private Flashcards (user-assigned)** — rejected. Users have no calibration anchor, so self-assigned scores are inconsistent across users and cards. Private cards are excluded from difficulty-aware features until a clear need and consistent scoring mechanism emerge.

**Mutable difficulty (re-scoring supported)** — deferred. As anchor calibration matures, re-grading a batch of cards may become necessary. Not supported in the initial implementation; revisit when the curriculum feature is built.

## Consequences

- `FlashcardDto` maps `difficulty` as `Int?` (nullable for Firestore deserialization). The DTO→domain mapper drops any card where `difficulty` is null.
- `Flashcard` domain model carries `difficulty: Int` — non-null, always valid.
- The filter runs only on the global pool path (`subcategories/{subcategoryId}/flashcards/`), not on `users/{uid}/privateCards/`.
- Seed tooling picks up `difficulty` from `inbox.jsonl` automatically — no explicit field mapping change needed.
- No `difficultyMin`/`difficultyMax` denormalization on Subcategory docs for now — deferred until a UI use case requires it.
- Difficulty anchors (`difficulty_anchors.json`) are local capture-tooling artifacts only; they never reach Firestore.
