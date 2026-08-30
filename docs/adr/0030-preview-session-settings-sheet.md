# Preview Study Session Screen presents session settings as a summary-row bottom sheet

The Preview Study Session Screen's bottom panel becomes a **persistent, no-scrim bottom
sheet** whose contents are one **summary row per adjustable setting** (Mode, Voice
answering, Voice/TTS, Length, Filters, Sort) plus the Start action — instead of laying
every control out flat. Each row shows its current value and opens a focused **dialog**;
the whole row is the tap target. This is the design record for that rework. Full design
detail: [docs/design/study-session-preview-sheet.md](../design/study-session-preview-sheet.md).

The panel had to expose mode, voice answering, TTS voice, session length, tag + difficulty
filters, and sort order before a session starts — too many controls to display flat. The
summary-row model keeps the surface compact and reads the current configuration at a
glance, while deferring each control to its own popup.

## Consequences

- **Voice answering is selectable on the Preview screen**, amending
  [ADR-0025](0025-voice-answering-rated-mode-only.md) (which had restricted it to an
  in-session toggle). The in-session toggle remains; the Rated interaction model itself is
  unchanged (manual self-rate, or full voice answering — no intermediate TTS-manual mode).
  `StudySessionRoute` carries the choice, and the session honours it on entry through the
  same consent-then-microphone path the in-session toggle uses.
- **Voice/TTS row visibility is mode-dependent**: shown for Fast, or Rated + voice
  answering on; hidden for plain manual Rated.
- **Filters reuse the merged tags + difficulty filter popup** from
  [ADR-0022](0022-subcategory-details-filter-sort-toolbar.md) (tags OR-within, AND-combined
  with a difficulty range). Tags are per-subcategory, so the tag facet appears for
  single-subcategory sessions only; multi-subcategory (Quick / Custom) sessions filter
  by difficulty only.
- **Persistence is "keep as default" per popup** (Mode, Voice answering, Length, Sort,
  Voice) — checked persists a global default, unchecked is session-scoped. The **Filters
  popup is exempt** (tags + difficulty always session-scoped), since a per-subcategory tag
  set can't carry to a different Subcategory.
- **Every popup is a dialog, on this screen and on the Settings screen alike** — the same
  `core:ui` dialog primitives, shell included, keyed on action count rather than on which
  screen opened them. A setting's popup shape therefore never depends on where it was
  opened from, and the merged Filters popup is shared whole with Subcategory Details.
