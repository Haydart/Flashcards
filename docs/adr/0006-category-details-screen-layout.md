# Category Details screen layout

## Top App Bar

`LargeTopAppBar` with gradient background and scroll-collapse, matching the Topic Details screen pattern. No hero block — the Android logo is dropped for now (may be added later as a subtle app bar background element).

**Default mode:** leading = back arrow `←`, title = category name, subtitle = "N topics · N cards", trailing = empty.

**Multi-select mode:** leading = close `✕`, title = category name, subtitle = empty. A grey label showing "N of N selected" is displayed below the app bar (same treatment as the selected-count label in Topic Details), not inside the bar itself.

We considered embedding the category icon and card count inside a custom hero block (as in the original mockup). Rejected because it is non-standard M3 and the `LargeTopAppBar` already handles the large-title-on-entry / compact-on-scroll pattern natively, eliminating the custom component.

## Bottom App Bar

`BottomAppBar` is always visible (both modes). It transforms between modes in-place.

**Default mode:**
- Actions slot: sliders icon — tapping enters multi-select (Composite Session) mode
- FAB slot: Extended FAB ⚡ "Quick Session" — launches Quick Session flow directly

**Multi-select mode:**
- Actions slot: select-all / deselect-all toggle icon — toggles between selecting all topics and deselecting all, based on current selection state
- FAB slot: Extended FAB ▶ "Start (N)" — disabled when 0 topics selected; N reflects current selection count

We considered placing Quick Session and the multi-select entry point as icon buttons in the top bar trailing slot (consistent with Topic Details' bookmark + filter icons). Rejected because "Quick Session" is the primary action on this screen and deserves Extended FAB weight, not an icon. The bottom bar also provides a natural transformation surface when multi-select activates.

## Entering and exiting multi-select

**Enter:** tap the sliders icon in the bottom bar actions slot.

**Exit:** tap `✕` in the top bar leading slot, or press system back (handled by `BackHandler`). Either action returns to default mode and deselects all topics.

We considered long-pressing a topic row to enter multi-select (familiar Android pattern). Rejected for discoverability — the sliders icon in the always-visible bottom bar makes the entry point explicit.

## Topic rows

Each row carries two independent tap zones.

**Default mode:**
- Leading slot: ▶ icon — tap triggers fast-start (single-topic Quick Session → Pre-start Screen)
- Row body tap: navigates to Subcategory Details
- Trailing: `›` chevron — also navigates to Subcategory Details (explicit affordance)

**Multi-select mode:**
- Leading slot: checkbox — swaps with ▶ in the same position
- Row body tap: toggles checkbox (primary action in this mode)
- Trailing: `›` chevron — still navigates to Subcategory Details even during multi-select

We considered hiding `›` during multi-select to reduce tap-target confusion. Rejected because navigating into a topic while building a composite session is a valid workflow, and removing the chevron would make it unreachable.

We considered a row-body tap in default mode doing nothing (requiring explicit `›` tap for navigation). Rejected — M3 list items are implicitly tappable; the larger tap target reduces friction with no conflict since ▶ is spatially isolated on the far left.

## Top bar trailing slot

Empty in both modes. No action icons surfaced there for now. An overflow `⋮` menu can be added later if secondary actions (sort, filter) are needed.
