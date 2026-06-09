# Flat, denormalized, untyped Tags (no `tags/` collection, no tag types)

A Tag is a single global, flat, user-facing keyword stored only as a denormalized string in each card's `tags[]` array. There is no `tags/` collection, no `type` field, and no internal/user-facing distinction. The filter-chip set on Subcategory Details is derived client-side as `distinct(card.tags)` over the subcategory's already-loaded cards. Private is a derived boolean flag (card lives under `users/{uid}/privateCards/`), not a Tag.

## Context

The original model (CONTEXT.md, early SYSTEMDESIGN) defined three tag kinds — **Specific** (per-subcategory, user-facing chips, with a mandatory "General"), **Common** (cross-subcategory umbrella, internal/AI-only), and **System** ("private", auto-applied) — backed by a `tags/{tagId} → { name, subcategoryIds[], type }` collection.

Reconciling against the actual capture corpus (`~/.claude/flashcards`, ~1920 cards) exposed three problems:

1. **Common Tags had no consumer.** The app has no cross-subcategory tag surface (Study screen is search-only; tag filtering exists only inside one Subcategory). "Internal/AI-facing only" was speculative with zero feature behind it.
2. **The captured data has no type structure** — just a flat `tags` array plus an inconsistent `category_path` breadcrumb whose last element is a theme. Mapping that onto three typed entities was pure import-time invention.
3. **The `tags/` collection carried no information.** After dropping `type` and `subcategoryIds[]` (tags are global; the chip set is derived), the doc reduced to `{ name }` where the name *is* the slug — a registry of nothing.

## Decision

- **One Tag kind.** Drop `type`, drop the Specific/Common/System distinction. Every Tag is global, flat, and user-facing; the same Tag (e.g. "state") may appear on cards across different Subcategories.
- **No `tags/` collection.** Tags exist only as `string[]` on each card. Chips are derived from the loaded card list. Display label = titlecased slug.
- **Private is a flag, not a Tag** — derived from collection membership, surfaced as a "Private" chip.

We considered keeping an almost-empty `tags/` registry for a future tag-management/rename feature. Rejected: Subcategory Details already loads all of a subcategory's cards, so derived chips are free; the app has no "all tags" screen and no rename flow; the registry would be dead weight plus a card↔registry sync burden.

## Consequences

- Significantly simpler Firestore schema and seed fixture (one fewer collection, no tag-card sync).
- No canonical tag registry → no global tag rename and no cross-subcategory tag listing. Neither is an MVP feature.
- Tag-soup cleanup (the captured `tags` carry ~1454 mostly-singleton values) is deferred to a later curation pass; cards import with their raw tag strings.
- Supersedes the "specific Tags" terminology referenced in [ADR-0001](0001-flat-two-level-taxonomy.md); in-Subcategory grouping is still done via Tags-as-chips, now untyped.
