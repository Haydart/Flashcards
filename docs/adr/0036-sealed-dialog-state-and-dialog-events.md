# Dialog state is one sealed field per screen, driven by one shared event

## Decision

A screen that owns dialogs carries **one nullable sealed field** naming the open dialog and holding
everything it needs, and exposes **one callback** for everything those dialogs report back.

The sealed dialog is the screen's whole dialog contract — the set it can show, and what each one
carries. Nothing restates that set:

```kotlin
typealias PreviewDialogEvent = DialogEvent<PreviewDialog>

sealed interface PreviewDialog {
    data class Mode(val draft: StudyMode, val keepAsDefault: Boolean = false) : PreviewDialog
    data class Filters(val draft: FlashcardFilters, val availableTags: List<String>) : PreviewDialog
    ...
}
```

The event type is **shared by every screen**, in `core:ui`, with one type parameter:

```kotlin
sealed interface DialogEvent<out D> {
    data class Open<out D>(val dialog: D) : DialogEvent<D>
    data class DraftChange<out D>(val dialog: D) : DialogEvent<D>
    data object Confirm : DialogEvent<Nothing>
    data object Dismiss : DialogEvent<Nothing>
}
```

Rules:

- **One `onDialogEvent: (XxxDialogEvent) -> Unit` parameter** on the content composable, not one
  lambda per dialog interaction.
- **A dedicated `XxxDialogHost` composable** in its own file holds the exhaustive `when` over the
  sealed dialog and renders it. It takes **exactly `activeDialog` and `onDialogEvent`** — everything
  a dialog needs to draw itself travels inside its own case, so the host never grows a parameter per
  dialog.
- **`Open` carries the dialog to show, already seeded by the caller.** A row that opens a dialog is
  already rendering the committed value it seeds from, so nothing has to be threaded in for it.
- **The ViewModel adds only what the call site could not**: open-time side effects, and a draft that
  lives outside screen state. Voice settings is the only such draft today — it comes from
  `VoiceSettingsController`'s saved settings and voice cache, so the ViewModel replaces the
  placeholder it is handed.
- **`DraftChange` carries the whole next dialog, built by the host.** The host's `when` has already
  narrowed to a concrete case, so it emits a total `copy()`. The ViewModel stores it in one line —
  no per-field event case, no narrowing cast.
- **The host may only do total field-level `copy()`.** No branching, no arithmetic, no derivation.
  Nothing unit-tests that file, so anything more belongs on the draft type instead, where a test can
  reach it — see `FlashcardFilters.withTag` and `StudySessionDialog.ReportProblem.withAction`.
- **`Confirm` and `Dismiss` carry no payload.** The ViewModel already holds the open dialog and its
  draft.
- **Side effects on a draft edit come from a ViewModel-side diff** of the previous and next dialog,
  not from a typed event naming the changed field. This keeps every dialog on the one generic
  `DraftChange` and the trigger unit-testable.
- **Navigating away from a dialog is a one-time event, never a host callback** (ADR-0019).
  Confirming "Exit session?" commits no draft; the ViewModel answers by emitting
  `StudySessionDestination.Back`.
- **A draft is UI state and never becomes a domain model.** It holds a domain value plus UI-only
  companions (`keepAsDefault`); confirming folds it into the domain type.
- **Nested cases are statically imported**, so call sites read `is ReportProblem` and
  `DraftChange(...)` rather than repeating the owner on every line.

This departs from the explicit-`onXxx`-lambda convention every other screen follows (AGENTS.md,
"ViewModel event handlers"), and applies **only** to dialogs. Ordinary screen callbacks stay
explicit lambdas.

## Context

The study session screen carried three independent dialog-visibility mechanisms: two boolean state
flags and a composable-local `var isExtendedContextDialogOpen by remember(card.id)` whose truth was
duplicated by a ViewModel field of its own, the two kept in sync by paired callbacks. The Preview
screen was about to grow five dialogs, each needing an open, a draft change, a confirm and a
dismiss.

The first two versions of this decision both kept a **parallel hierarchy of `Open` cases** mirroring
the dialog hierarchy — the same five names written twice, kept in sync by hand — because seeding a
draft was assumed to need ViewModel state the button firing the event lacked. Checking every call
site showed that assumption was wrong: nine of ten already render the value they would seed from, or
sit inside the null check that guards it. Only voice settings genuinely cannot seed itself.

The first version additionally gave every draft field its own `DraftChange` case, forcing each
ViewModel to carry a
`private inline fun <reified T : XxxDialog> updateActiveDialog(transform: (T) -> T)` helper whose
cast could silently drop an event. All of it is gone now that `Open` and `DraftChange` both carry a
whole dialog.

Note that the parameter count which originally motivated the single callback is much smaller than
first estimated — with a generic `DraftChange` and no `Open` list, five dialogs would be a handful
of lambdas rather than twenty. The single callback is kept because it is still fewer parameters and
fewer no-op lambdas in every `@Preview`, not because the alternative is unworkable. On a screen with
one dialog it is consistency, not necessity.

## Alternatives considered

**One nullable field per dialog, keeping explicit lambdas** — rejected. Two dialogs open at once
stays representable, every dialog adds four parameters, and discarding a draft on dismiss becomes
manual bookkeeping in each dismiss handler.

**A `DialogState` object with a `type` enum plus a bag of nullable draft fields** — rejected. Every
draft field is nullable regardless of which dialog is open, so every read needs a null check the
type system could have made unnecessary.

**A per-field `DraftChange` case per draft field** — the original form of this ADR, rejected on
revision. It mirrors the dialog hierarchy and buys nothing the host's already-narrowed `copy()`
does not.

**A typed `Open` hierarchy per screen** — the second form of this ADR, rejected on revision. It is a
parallel list of the same names the sealed dialog already declares.

**A nested `Key` marker carried by each case's `companion object`** — rejected. It removes the
parallel list but replaces it with per-case boilerplate, and forces a second type parameter on the
shared event so `Open` can be typed.

**An `enum class XxxDialogKey`** — rejected for the same reason as the typed `Open` hierarchy: it is
the same parallel list in a different shape, and still needs a second type parameter.

**Keeping the draft in the composition via `rememberSaveable`** — rejected. It would survive process
death, which a modal dialog does not need, and it moves seeding, discard-on-dismiss and the draft
itself out of the ViewModel, costing roughly seven unit tests that would have to be replaced with
Compose UI tests.

**Treating dialog-open as a one-time event like navigation (ADR-0019)** — rejected. Navigation is
one-time because the destination is durable state in the back stack; a dialog has no such backing
store, so a consumed-once event leaves visibility in the composition, and `Confirm` has no draft to
fold. A dialog is state. Its *outcome* may be a one-time event, and is.

**A single global `AppDialog` plus one shared host** — rejected. It would cut a dialog's cost from
about twelve lines per screen to two, but any screen could then show any dialog, and the set becomes
a junk drawer as screen-specific dialogs accumulate. The per-screen sealed set is worth keeping as
legible documentation of what a screen shows.

**Dialog contracts as interfaces implemented by ViewModels** (`VoiceSettingsDialogHost`) — rejected.
A ViewModel hosting two shared dialogs cannot have two `onConfirm()`, so every method needs a
dialog-specific prefix: five prefixed public methods per shared dialog, which is the per-dialog
naming the single callback exists to avoid, relocated onto the ViewModel's public surface.

**Splitting `Open` onto its own callback** — rejected. It gives the cleanest shared type, but the
screen then carries two dialog callbacks, and `Open` carrying a seeded dialog achieves the same
sharing with one.

**A `Defaultable<T>` sub-interface for the four `(draft, keepAsDefault)` dialogs** — rejected.
`copy()` is not polymorphic, so each case would need hand-written `withDraft`/`withKeepAsDefault`.
That is more boilerplate, and it costs the `copy()` that makes the generic `DraftChange` typesafe.

**A shared per-dialog slot composable** for dialogs used on more than one screen — deferred, not
rejected. It saves about four lines per site today against one more indirection; revisit when sort
and filters land on a second screen and the pattern is four sites rather than two.

**Scope-receiver DSL for dialog content** — rejected. It introduces a pattern that exists nowhere
else in this codebase for no gain over an exhaustive `when`.

## Consequences

- **Discard-on-dismiss is free.** The draft dies with the field, so `Dismiss` is one assignment.
- **A new dialog does not compile until it is wired**, because the host's `when` is exhaustive. This
  is the load-bearing guarantee, and it survives every simplification above.
- **Two dialogs open at once is unrepresentable** — they are modal anyway.
- **A draft edit is correct by construction.** The host copies a case the `when` already narrowed,
  so there is no cast to mismatch and no event that can be dropped.
- **A draft field that only some cases carry is enforced at compile time.** `PreviewDialog.Filters`
  has no `keepAsDefault`, so the host cannot emit one for it — a runtime guard the ViewModel used to
  need became a compile error.
- **A dialog cannot open in an impossible state.** The extended-context dialog is opened from inside
  the null check on the card's text, so "open with nothing to show" stopped being expressible.
- **Seeding is no longer unit-tested.** It moved from a `when` in the ViewModel to a one-liner at the
  call site, directly beneath the line rendering the same value. Four tests were deleted rather than
  kept as tautologies asserting the ViewModel echoes back what the test constructed. This is the real
  price of the decision and it is not recovered anywhere.
- **The voice-settings draft lives in `activeDialog` like every other draft.**
  `VoiceSettingsController` keeps the voice-list cache, preview playback and saving, but no longer
  owns the draft; audio preview fires from the ViewModel's draft diff, where a unit test can reach
  it. Its case carries a defaulted placeholder the ViewModel always replaces.
- **One narrowing cast remains**, in each ViewModel's async voice-list load, which has to find the
  open dialog to fill in. Once per open rather than once per edit; a dismissal in the meantime
  correctly drops it.
- ADR-0020's argument order is unaffected — `onDialogEvent` sits with the other callbacks.
- Applies on `PreviewStudySessionScreen`, `StudySessionScreen` and `SettingsScreen` as of this ADR.
