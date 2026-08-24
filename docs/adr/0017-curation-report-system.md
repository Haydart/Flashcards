# Report a problem: single collection, action map per card, universal in-session entry point

> **Amended when the dialog system landed.** Three clauses below changed:
> 1. **The draft always starts empty and Submit is additive.** The sheet no longer seeds from the
>    card's stored report, so unchecking a row means "not reporting this now", never "withdraw".
>    Submit upserts the checked actions and removes nothing. **There is consequently no in-app way
>    to withdraw a report** — `CurationRepository.removeCurationAction` survives as the primitive,
>    with no caller. Withdrawal is an admin/sync-tooling concern until a screen needs it.
>    Rationale: seeding makes an unchecked box ambiguous between "not a problem" and "already
>    reported", and lazily fetching every card's report state to populate it costs a read for a
>    feature most sessions never touch.
> 2. **It is a dialog, not a bottom sheet**, built on `FlashcardsDecisionDialog` — Cancel discards
>    the whole draft, Submit commits it.
> 3. **The entry point is a flag `IconButton` in the study session top bar**, left of the card
>    counter, enabled only while a card is displayed — the bottom sheet is already crowded with the
>    mic toggle, the cog and the transport controls. It ships to production; the previous
>    `BuildConfig.DEBUG` FAB and its dialog are deleted.
>
> Also: `WrongTags` joined the `CurationAction` enum (seven actions, all reportable), and Submit
> writes them through `SubmitCurationReportUseCase` -> `CurationRepository.upsertCurationActions`,
> preserving this ADR's "one write on Submit".

## Decision

Any user can flag a Flashcard for a content fix via the flag icon on a study session card (Rated and
Fast alike). Tapping it opens a **"Report a problem"** sheet listing 6 Curation Actions the user can
toggle on/off for the current card, plus Cancel/Submit. Toggling only updates local sheet state;
Submit writes the checked actions to `users/{uid}/curationRequests/{cardId}` as a map of action key →
`{ flaggedAt: Timestamp }` in one write, Cancel discards the local changes. Multiple actions can be
active on a single card simultaneously, each independently toggleable — unchecking one and submitting
withdraws it, and the document is deleted once the last action is removed. Reported Flashcards are
**not suppressed** — they continue appearing in Study Sessions normally. Curation Requests are
consumed by admin sync scripts; there is no user-facing management or withdraw screen — withdrawing a
reason is done only by reopening the sheet on that card, unchecking it, and submitting.

## Context

The global Flashcard pool is large and AI-generated. Content issues (wrong difficulty, missing code
formatting, factual errors) are discovered while studying but have no in-session repair path. A
report mechanism bridges the gap between discovering an issue mid-session and queueing a precise fix
for the sync script.

## Alternatives considered

**Two-action model (Retire / Rework), with a dedicated management screen for viewing and withdrawing
past reports** — rejected. A coarse two-value signal loses the specificity needed for automated
fixes, and a management screen is unneeded scope while there's no user-facing use for reviewing past
reports.

**Separate collections for casual user reports vs. structured fix directives** — rejected. One
taxonomy, one admin pipeline; splitting them doubles the sync-script surface for no signal-quality
gain.

**Bulk report action from a card list (multi-select multiple cards, apply one action to all)** —
rejected. A single bulk action can't express "this subset needs X, that subset needs Y" across six
independent per-card reasons; reporting stays a per-card, in-session action only.

**Speed-dial mini-FABs instead of a sheet** — rejected. 6 actions exceed the practical limit for
speed-dial (typically ≤5), and labels are essential to distinguish similar-sounding actions. A sheet
with icon + label per action is cleaner.

**Eager per-card fetch of report state** — rejected. One Firestore read per card advance is wasteful
when the feature may never be used in a session. Lazy batch fetch on first flag-icon tap amortizes
cost to a single read only when needed.

**Delta-based difficulty change (`+1`/`-1`)** — rejected. A card may be severely mismatched. Storing
direction only (`DIFFICULTY_TOO_EASY` / `DIFFICULTY_TOO_HARD`) lets the AI agent assign an appropriate
new value rather than nudging incrementally.

**Auto-resume voice after sheet dismiss** (Fast/voice-answering sessions) — rejected. User paused to
report a card; auto-resuming overrides that intent. One manual tap to resume is an acceptable UX cost.

**Empty document when all actions removed** — rejected. Empty report docs have no meaning and create
noise in the sync script's query results. Delete doc on last action removal.

## Consequences

- `users/{uid}/curationRequests/{cardId}` → `{ subcategoryId: String, actions: { "<CurationAction>": { flaggedAt: Timestamp } } }`
- `CurationAction` values: `DIFFICULTY_TOO_EASY`, `DIFFICULTY_TOO_HARD`, `DELETE`, `BACKTICK_REDO`, `NEEDS_CODE_EXAMPLE`, `FULL_REDO`. Presented to users as: "Too easy," "Too hard," "Duplicate or low quality," "Formatting looks broken," "Needs a code example," "Needs a full rewrite."
- `DIFFICULTY_TOO_EASY` and `DIFFICULTY_TOO_HARD` are mutually exclusive — map key semantics enforce this (writing one overwrites the other). All other actions can coexist.
- Report state is loaded lazily on first flag-icon tap via a batched `whereIn` query (chunks of 30 due to Firestore limit) and cached in `StudySessionViewModel` for the session.
- Writes happen on Submit: checked Curation Actions are upserted to Firestore in one write; Cancel discards local toggle changes; snackbar error shown on write failure.
- `CurationRepository` is the sole interface for this concern — separate from `FlashcardRepository`.
- No management screen: Curation Requests are invisible to users anywhere outside the report sheet itself.
- No bulk report action anywhere in the app (e.g. Subcategory Details has no multiselect report toolbar).
