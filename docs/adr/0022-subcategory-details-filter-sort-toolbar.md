# Subcategory details: mandatory difficulty, merged filter sheet, bottom toolbar

## Decision

`Flashcard.difficulty: Int` (1-10) becomes mandatory for **all** Flashcards, including Private ones. This supersedes the "Difficulty on Private Flashcards — rejected" alternative in [ADR-0010](0010-difficulty-field-design.md). The Private Flashcard creation dialog requires a difficulty value (Slider, 1-10, discrete steps) before a card can be saved. No backfill is needed: no Private Flashcards exist in Firestore yet, so the mandatory field applies from day one with no transition period for the private-card path (the global-pool backfill/filter behavior described in ADR-0010 is unaffected and still applies to admin-curated cards).

SubcategoryDetailsScreen's top app bar is reduced to: back navigation + bookmark action + overflow menu. The overflow menu is rendered even though it currently has no items, as a deliberate placeholder for a near-future item (creating a dynamic launcher shortcut for the subcategory, see `docs/design/launcher-shortcuts.md`) — this is an intentional exception to the general rule that an overflow affordance shouldn't open onto an empty menu.

Filtering and sorting move out of the top bar and into a bottom toolbar matching the visual layout already established in the Category details design (rounded floating toolbar with a docked/adjacent FAB region; the exact composable — stock M3 `BottomAppBar` vs an M3 Expressive floating toolbar — is an implementation detail to be decided when built). Left-to-right on the toolbar: Filter icon, Sort icon, Add(+) icon, then the FAB (extended pill, "Start session", primary action) on the trailing end.

Filter is a **single** entry point (not one icon per facet): tapping it opens one sheet containing both the existing Tags multi-select chips and a new Difficulty `RangeSlider` (1-10). The two facets combine with AND (a card must match the tag selection, if any, AND fall inside the difficulty range, if set); multiple selected tags still combine with OR among themselves, unchanged from the existing behavior. Sort is a separate icon opening a menu with three mutually-exclusive options: Default (original list order), Easiest first, Hardest first — not a tap-to-cycle icon, so the current state is always legible from an explicit menu rather than inferred from icon glyph alone.

Both the Filter and Sort icons show a small badge dot whenever their state is non-default, consistent with the existing filter-active indicator already used elsewhere in the app.

The existing "N OF M CARDS FILTERED" banner reflects filter count only; it does not describe sort state, since sort never changes how many cards are shown, only their order. Sort's own state is surfaced solely via its badge dot.

Filtering to zero results is a reachable state once tag and difficulty filters can combine (e.g., a rare tag with a narrow difficulty range). This requires a dedicated empty-filtered-results state (distinct from the existing loading/error states), with messaging plus a "clear filters" action, and the Start-session FAB must disable when the filtered set is empty. This state is **designed but not yet implemented** — `SubcategoryDetailsScreenState` still only models `isLoading` / `error` / `flashcards` as of this writing.

## Context

The Subcategory details screen (`SubcategoryDetailsScreen.kt`) was a placeholder: plain button, no filtering, no sorting, no bottom toolbar. This ADR is the design record for reworking it to match the target Figma ("Subcategory details + Create card") alongside the Category details bottom toolbar pattern, adding tag filtering (UI already speced), difficulty sorting, and difficulty filtering.

Difficulty-aware sorting/filtering surfaced a gap ADR-0010 anticipated but deferred: Private Flashcards had no difficulty and were explicitly excluded from "difficulty-aware features until a clear need... emerges." Subcategory details' mixed list of global + private cards is exactly that need. Rather than build exemption logic (private cards pinned to list-end on sort, dropped from filter results), forcing every card — private included — to carry a difficulty removes an entire class of edge cases from sort/filter logic, at the cost of one more mandatory field in the creation dialog.

## Alternatives considered

**Two separate filter icons (Tags, Difficulty)** — rejected. Pushes left-side icon count to 4 (plus FAB), crowds a phone-width toolbar, and reads as two things named "filter" without a grouping label.

**Sort as a tap-to-cycle icon** — rejected. Hides current state behind icon-glyph interpretation; an explicit menu keeps state legible per M3 guidance.

**Private cards exempt from difficulty (keep ADR-0010 as-is), pinned-to-end on sort / dropped from filter results** — rejected. No private cards exist in Firestore yet, so there's no migration cost to avoid; forcing the field is simpler than building and maintaining exemption logic in both sort and filter paths indefinitely.

**Backfill-enforce difficulty on existing private cards** — moot; not applicable since none exist yet.

**Fold sort state into the "N OF M CARDS FILTERED" banner** — rejected. Conflates two independent axes (count vs order) into one line; sort's badge dot is sufficient.

**Hide the overflow menu until it has an item** — rejected for this specific case, despite being the generally-correct M3 default; kept visible now because a concrete near-future item (dynamic launcher shortcut) is already planned.

## Consequences

- `FlashcardDto`/creation flow must validate `difficulty` is present (1-10) before writing a new Private Flashcard document; the mapper's null-filtering behavior from ADR-0010 remains for the global-pool backfill path only.
- Private Flashcard creation dialog UI requires a `Slider` control (1-10, discrete steps) wired as a mandatory field, not optional.
- `SubcategoryDetailsScreenState` needs new fields for active tag selection, active difficulty range, active sort mode, and (pending implementation) a distinct empty-filtered-results case.
- No exemption/fallback logic is needed anywhere for cards lacking difficulty, since none can exist going forward.
- Empty-filtered-results state and FAB-disable-on-zero-results are specified here but left as a follow-up implementation task.
