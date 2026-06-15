# Firestore collection structure: subcollections for cards, flat subcategories, namespaced subcategory IDs

## Decision

Flashcards are stored as a subcollection directly under their Subcategory document: `subcategories/{subcategoryId}/flashcards/{cardId}`. There is no separate `cards/` top-level collection. Categories and Subcategories are separate top-level collections. Subcategory document IDs are namespaced as `{categoryId}-{subSlug}` (e.g. `android-testing`). The `subcategoryId` field is not stored on Flashcard documents — it is encoded in the collection path.

## Context

Every read of Flashcards is scoped to one or more Subcategories (Subcategory Details, Pre-start Screen card selection, Quick Session). The original flat `cards/{cardId}` collection required a `WHERE subcategoryId == X` query and a Firestore composite index for every such read.

Subcategories were originally stored in the `categories/` collection alongside Categories, distinguished by a `parentId` field. Direct lookup by subcategoryId (required for Favorites and Recents on the Home screen, which store only `subcategoryId`) worked cleanly, but Category + Subcategory were conflated in a single collection.

## Alternatives considered

**Flat `cards/{cardId}` with WHERE query** — rejected. Every card fetch requires an indexed query. No structural grouping. Index management overhead as the corpus grows.

**Separate `cards/{subcategoryId}/flashcards/{cardId}` top-level collection** — rejected. Creates phantom container documents with no fields mirroring the `subcategories/` collection exactly. Redundant top-level collection dropped in favour of using the Subcategory doc itself as the flashcard container.

**Subcategory subcollection under categories (`categories/{catId}/subcategories/{subId}`)** — rejected. Fetching a Subcategory doc requires knowing the parent `categoryId` to construct the path. Favorites and Recents store only `subcategoryId` — they cannot construct the path without an extra lookup or fragile slug parsing.

**Bare subcategory slugs as IDs (`testing`, `compose`)** — rejected. Subcategory slugs are only unique within a Category. A `testing` Subcategory under both Android and Python would collide in a flat `subcategories/` collection.

## Consequences

- Fetching all Flashcards for a Subcategory: single `getDocuments()` on `subcategories/{subcategoryId}/flashcards` — no WHERE, no index.
- Multi-subcategory fetch (Quick Session, composite Pre-start): N parallel `getDocuments()` calls, one per Subcategory.
- `subcategoryId` field dropped from Flashcard documents — redundant with path, cannot drift out of sync.
- Favorites and Recents look up Subcategory docs directly: `subcategories/android-testing` — one read, no join.
- Category Details loads Subcategories via `subcategories.where("categoryId", "==", "android")` — single indexed query.
- Subcategory IDs (`android-testing`) are stable and predictable. Admin seed tooling constructs them deterministically from `{categoryId}-{subSlug}`.
- `cardCount` is a denormalized field on each Subcategory doc and must be updated when Flashcards are added or removed. `subcategoryCount` on Category docs must be updated when Subcategories are added.
- `categoryName` is a denormalized field on each Subcategory doc (mirrors the parent Category's display name) to avoid a join when displaying the subcategory in isolation.
- `difficulty` is a mandatory integer field (1–10) on global Flashcard documents. Documents missing this field are filtered at the DTO layer and never reach the domain. See ADR-0010 for the full design rationale.
