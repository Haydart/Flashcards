# Firestore collection structure: subcollections for cards, flat subcategories, namespaced subcategory IDs

## Decision

Flashcards are stored as a subcollection directly under their Subcategory document: `subcategories/{subcategoryId}/flashcards/{cardId}`. There is no separate `cards/` top-level collection. Categories and Subcategories are separate top-level collections. Subcategory document IDs are namespaced as `{categoryId}-{subSlug}` (e.g. `android-testing`). The `subcategoryId` field is not stored on Flashcard documents — it is encoded in the collection path.

## Context

Every read of Flashcards is scoped to one or more Subcategories (Subcategory Details, Preview Study Session Screen card selection, Quick Session). The original flat `cards/{cardId}` collection required a `WHERE subcategoryId == X` query and a Firestore composite index for every such read.

Subcategories were originally stored in the `categories/` collection alongside Categories, distinguished by a `parentId` field. Direct lookup by subcategoryId (required for Favorites and Recents on the Home screen, which store only `subcategoryId`) worked cleanly, but Category + Subcategory were conflated in a single collection.

## Alternatives considered

**Flat `cards/{cardId}` with WHERE query** — rejected. Every card fetch requires an indexed query. No structural grouping. Index management overhead as the corpus grows.

**Separate `cards/{subcategoryId}/flashcards/{cardId}` top-level collection** — rejected. Creates phantom container documents with no fields mirroring the `subcategories/` collection exactly. Redundant top-level collection dropped in favour of using the Subcategory doc itself as the flashcard container.

**Subcategory subcollection under categories (`categories/{catId}/subcategories/{subId}`)** — rejected. Fetching a Subcategory doc requires knowing the parent `categoryId` to construct the path. Favorites and Recents store only `subcategoryId` — they cannot construct the path without an extra lookup or fragile slug parsing.

**Bare subcategory slugs as IDs (`testing`, `compose`)** — rejected. Subcategory slugs are only unique within a Category. A `testing` Subcategory under both Android and Python would collide in a flat `subcategories/` collection.

## Consequences

- Fetching all Flashcards for a Subcategory: single `getDocuments()` on `subcategories/{subcategoryId}/flashcards` — no WHERE, no index.
- Multi-subcategory fetch (Quick Session, composite Preview Study Session): N parallel `getDocuments()` calls, one per Subcategory.
- `subcategoryId` field dropped from Flashcard documents — redundant with path, cannot drift out of sync.
- Favorites and Recents look up Subcategory docs directly: `subcategories/android-testing` — one read, no join.
- Category Details loads Subcategories via `subcategories.where("categoryId", "==", "android")` — single indexed query.
- Subcategory IDs (`android-testing`) are stable and predictable. Admin seed tooling constructs them deterministically from `{categoryId}-{subSlug}`.
- `cardCount` is a denormalized field on each Subcategory doc and must be updated when Flashcards are added or removed. `subcategoryCount` on Category docs must be updated when Subcategories are added.
- `categoryName` is a denormalized field on each Subcategory doc (mirrors the parent Category's display name) to avoid a join when displaying the subcategory in isolation.
- `difficulty` is a mandatory integer field (1–10) on every Flashcard document. Documents missing this field are filtered at the DTO layer and never reach the domain (a global-pool backfill concern only — see ADR-0010 for the full design rationale).
- `extendedContext` is a nullable string field on global Flashcard documents. Omitted on simple cards (difficulty 1–3) where the Q&A is self-explanatory; present on mid/hard cards (4–10) with progressively richer teaching material. Never duplicates the `answer` field.
- `color` (hex string, e.g. `"#6B2FA0"`) and `iconSvg` (inline plain SVG text, not a URL, rendered on-device via `androidsvg`) are optional fields on Category documents — nullable, since the seed pipeline may auto-create a category before either is curated; rendering falls back to a themed default color and a generic glyph when either is absent or malformed. Neither field lives in Firebase Storage. Neither field exists on Subcategory documents — icon+color are Category-level only. See `docs/design/category-icon-color.md` for the full rendering design and rationale.
- `topSubcategoryNames` (list of up to 5 Subcategory display names) is a denormalized field on Category documents, computed once by `build_fixture.py` from the same card-volume ordering that produces each Subcategory's `order` field. Unlike `color`/`iconSvg`, it is **not sticky** — every reseed overwrites it unconditionally, since it's pure derived data with no manual curation. Exists so the Browse screen's default-state topic-summary chips ("Compose · Coroutines · Compose Navig…") and category search results can be rendered from a single `categories` read (N reads, N = category count) without ever touching the `subcategories` collection.
- `nameLower` (lowercased `name`) is a denormalized field on Subcategory documents, added to support case-insensitive prefix-range search queries (`nameLower >= query AND nameLower < query + ""`) — Firestore range comparisons are case-sensitive and it has no substring/"contains" query, so search is prefix-only by construction. See `docs/design/category-search.md` for the full search design.
