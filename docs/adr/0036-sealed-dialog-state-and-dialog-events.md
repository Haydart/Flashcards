# Dialog state is one sealed field per screen, driven by one sealed event

## Decision

A screen that owns dialogs carries **one nullable sealed field** naming the open dialog and holding
its draft, and exposes **one callback** for everything those dialogs report back:

```kotlin
data class PreviewStudySessionScreenState(
    ...,
    val activeDialog: PreviewDialog? = null,
)

sealed interface PreviewDialog {
    data class Mode(val draft: StudyMode, val keepAsDefault: Boolean = false) : PreviewDialog
    data class Filters(val draft: FlashcardFilters) : PreviewDialog
    ...
}
```

```kotlin
sealed interface PreviewDialogEvent {
    sealed interface Open : PreviewDialogEvent { data object Mode : Open; ... }
    sealed interface DraftChange : PreviewDialogEvent { data class Mode(val mode: StudyMode) : DraftChange; ... }
    data object Confirm : PreviewDialogEvent
    data object Dismiss : PreviewDialogEvent
}
```

Rules:

- **One `onDialogEvent: (XxxDialogEvent) -> Unit` parameter** on the content composable, not one
  lambda per dialog interaction.
- **A dedicated `XxxDialogHost` composable** in its own file holds the exhaustive `when` over the
  sealed dialog and renders it.
- **`Confirm` and `Dismiss` carry no payload.** The ViewModel already holds the open dialog and its
  draft; handing the value back would create a second source of truth.
- **Events are grouped into `Open` and `DraftChange`** so the ViewModel dispatches in three small
  exhaustive `when`s. Exhaustiveness is the reason the hierarchy is sealed, so it is never traded
  for an `else` branch.
- **A draft is UI state and never becomes a domain model.** It holds a domain value plus UI-only
  companions (`keepAsDefault`); confirming folds it into the domain type.

This departs from the explicit-`onXxx`-lambda convention every other screen follows (AGENTS.md,
"ViewModel event handlers"), and applies **only** to dialogs. Ordinary screen callbacks stay
explicit lambdas.

## Context

The study session screen carried three independent dialog-visibility mechanisms: two boolean state
flags and a composable-local `var isExtendedContextDialogOpen by remember(card.id)` whose truth was
duplicated by a ViewModel field of its own, the two kept in sync by paired callbacks. The Preview
screen was about to grow five dialogs, each needing an open, a draft change, a confirm and a
dismiss — roughly twenty parameters on a content composable already suppressed in the detekt
baseline for `LongParameterList`, plus twenty no-op lambdas in every `@Preview`.

## Alternatives considered

**One nullable field per dialog, keeping explicit lambdas** — rejected. Two dialogs open at once
stays representable, every dialog adds four parameters, and discarding a draft on dismiss becomes
manual bookkeeping in each dismiss handler.

**A `DialogState` object with a `type` enum plus a bag of nullable draft fields** — rejected. Every
draft field is nullable regardless of which dialog is open, so every read needs a null check the
type system could have made unnecessary.

**Scope-receiver DSL for dialog content** — rejected. It introduces a pattern that exists nowhere
else in this codebase for no gain over an exhaustive `when`.

## Consequences

- **Discard-on-dismiss is free.** The draft dies with the field, so `Dismiss` is one assignment.
- **A new dialog does not compile until it is wired**, because the host's `when` is exhaustive.
- **Two dialogs open at once is unrepresentable** — they are modal anyway.
- **Confirm handlers become uniform**: fold the draft into the committed value, persist the default
  if asked, re-run derived computation, clear the field.
- Narrowing casts appear in ViewModel handlers, paid once per ViewModel via a
  `private inline fun <reified T : XxxDialog> updateActiveDialog(transform: (T) -> T)` helper that
  ignores an event whose dialog is no longer open — a mismatch means a race with dismissal, and
  dropping it is correct.
- `VoiceSettingsDraftState` in `core:ui` lost its `isVisible` flag: a shared controller injected by
  two ViewModels cannot own visibility once the screen does.
- ADR-0020's argument order is unaffected — `onDialogEvent` sits with the other callbacks.
- Applies on `PreviewStudySessionScreen`, `StudySessionScreen` and `SettingsScreen` as of this ADR.
