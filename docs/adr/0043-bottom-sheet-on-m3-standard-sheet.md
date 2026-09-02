# Bottom sheet rebuilt on M3's standalone `BottomSheet`, narrowing the no-drag-handle rule to modal sheets

## Decision

`FlashcardsBottomSheet` is rebuilt on Material 3's standalone `BottomSheet` (new in the 1.5 line,
already pinned at `material3 = "1.5.0-alpha23"`), replacing the hand-rolled docked `Surface` it
was before. It had zero consumers — added for the Preview study session screen and never wired
up — so the reshape is a clean break, not a migration.

**New shape:** the caller owns a `SheetState` (built with `rememberFlashcardsBottomSheetState`)
and an `onDismissRequest`, plus `draggable` and `dismissible` flags, plus one content slot. No
header slot, no pinned-actions region — most sheets that follow this one will not have pinned
actions at all, and the caller is better placed to lay out its own contents than a shared
component guessing at a shape. `minHeight` is gone with it.

**State is hidden/expanded only** — `rememberFlashcardsBottomSheetState` never enables
`PartiallyExpanded`, so there is no half-open anchor for callers to reason about and no peek
height to compute.

**`dismissible = false` drops hidden from the enabled set**, not just from the visible affordances.
`BottomSheet`'s own contract makes `SheetState.hide()` throw once hidden is excluded, so the
wrapper's job is making sure none of its own codepaths can hit that: `backHandlerEnabled` is wired
straight to `dismissible` (a disabled predictive-back handler never calls the internal
`hide()`-driven dismiss path), and `rememberFlashcardsBottomSheetState` attaches a
`confirmValueChange` veto on `Hidden` for the same case — needed because `BottomSheet`'s drag
physics define a hidden anchor for the gesture to rubber-band against regardless of the enabled
set, so `enabledValues` alone does not stop a physical swipe from settling there.

**`draggable` maps straight to `gesturesEnabled`.**

**Predictive back, the bottom system-bar inset, and the drag handle's expand/collapse/dismiss
accessibility actions are not re-implemented** — they come from `BottomSheet` itself. The
`navigationBarsPadding()` the old hand-rolled version applied by hand is gone; the component's own
`contentWindowInsets` default (bottom safe-drawing only) covers it.

**Drag handle rule narrowed to modal sheets.** The house rule against bottom-sheet drag handles
applies to *modal* sheets, where the scrim and back gesture already communicate that the surface
is dismissible. It does not apply to standalone/docked sheets like this one: there is no scrim, so
the handle is the only signal the surface moves at all. `FlashcardsBottomSheet` keeps M3's default
`BottomSheetDefaults.DragHandle()`.

**Height stays the caller's problem.** The expanded anchor is derived from the content's measured
height; content taller than the screen pins at the top and clips rather than scrolling. No
percentage ceiling is imposed. A caller with variable-length content makes its own content column
scrollable — documented on `FlashcardsBottomSheet`'s KDoc rather than enforced, since the component
has no way to know which callers need it.

## Rejected alternative: `BottomSheetScaffold`

`BottomSheetScaffold` is the only public M3 API offering an arbitrary peek height, but nothing
here needs a peek. It would also take over the whole screen — it owns the top bar, the body and a
snackbar host — and paints an opaque container colour over the caller's own modifier, which a
gradient screen (the Preview study session screen this component was built for) would then have to
work around. Rejected on both counts.

## Rejected alternative: `ModalBottomSheet`

Would bring back exactly what this component exists to avoid: a separate `Dialog` window and a
scrim, neither of which fits a permanently-docked settings panel that composes alongside its
screen's own content.

## Consequences

- `FlashcardsBottomSheet`'s Showkase entry moved from a private preview-only function to a public
  `@ShowkaseComposable`, matching `core/ui/README.md`'s stated convention (previously the file
  stacked `@ShowkaseComposable` and `@PreviewLightDark` on one private function); a separate
  private `@PreviewLightDark` covers both themes as its own function.
- Every M3 `BottomSheet`/`SheetState` call site in `core:ui` needs
  `@OptIn(ExperimentalMaterial3Api::class)` until the API graduates.
- No existing call sites to migrate. The Preview study session screen's settings sheet (and any
  future study-session/summary-screen sheet) is the first real consumer.
