# Flat denormalized untyped tags — no tags/ collection

## Decision

Tags are stored as plain strings in a `tags[]` array on each Flashcard document. There is no `tags/` collection, no global tag registry, and no tag type stored in Firestore. Tag kind (Specific, Common, System) is a product-level concept only — the distinction is enforced by the capture skill and admin tooling, not by the data model.

## Context

Tags serve two purposes: user-facing filter chips on Subcategory Details (Specific Tags), and internal/AI classification signals (Common Tags). A System Tag (`"private"`) is auto-applied to Private Flashcards. The question was whether to back these with a Firestore registry or treat them as bare strings.

## Alternatives considered

**`tags/{tagId}` collection with `subcategoryIds[]` and `type` field** — rejected. Adds a collection that must be kept in sync with every card write. Tag filter chips on Subcategory Details are derived client-side from `distinct(card.tags)` over the loaded cards — a registry adds no query benefit for that use case. Tag rename (updating a name across all cards) is an admin-only operation rare enough to handle via a migration script, not a live Firestore feature.

**Per-subcategory tag subcollections** — rejected. Over-engineers a feature that is entirely read-driven client-side. Adds write complexity to every card import and Private Flashcard creation.

## Consequences

- No global tag rename without a corpus migration script.
- New tags appear automatically as cards are imported — zero admin overhead.
- Filter chips on Subcategory Details are derived from loaded cards (`distinct(card.tags)`) — no extra Firestore read.
- Tag kind is invisible in Firestore; Common Tags simply never surface in filter chip UI by convention in the Android layer.
- The `"private"` System Tag is derived from collection membership (`users/{uid}/privateCards/`) and surfaced as a chip separately — it is not stored in `tags[]`.
