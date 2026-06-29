# Debug curation: separate collection, action map per card, debug-build-only FAB in Study Session

## Decision

A debug-only curation tool is surfaced as a FAB on `StudySessionScreen`. Tapping it opens a dialog listing 6 Curation Actions the developer can toggle on/off for the current card. Actions are stored at `users/{uid}/curationRequests/{cardId}` as a map of action key → `{ flaggedAt: Timestamp }`. Multiple actions can be active on a single card simultaneously. The feature is gated behind `BuildConfig.DEBUG`. Curation Requests are consumed by admin sync scripts — they are not shown on the Flags Screen and do not interact with `flaggedCards`.

## Context

The global Flashcard pool is large and AI-generated. Content issues (wrong difficulty, missing code formatting, factual errors) are discovered while studying but have no in-session repair path. The existing `flaggedCards` system (ADR-0009) provides coarse `RETIRE`/`REWORK` signals for users, but offers no structured fix directives for admin/tooling use. A dedicated dev-only curation tool bridges the gap between discovering an issue mid-session and queueing a precise fix for the sync script.

## Alternatives considered

**Extend `flaggedCards` with new action strings** — rejected. `flaggedCards` is user-facing and rendered on the Flags Screen. Adding dev-only action types pollutes that query and couples two distinct lifecycles (user feedback vs. content maintenance tooling).

**Speed-dial mini-FABs instead of dialog** — rejected. 6 actions exceed the practical limit for speed-dial (typically ≤5), and labels are essential to distinguish `BACKTICK_REDO` from `FULL_REDO`. Dialog with icon + label per action is cleaner.

**Eager per-card fetch of curation state** — rejected. One Firestore read per card advance is wasteful when the feature may never be used in a session. Lazy batch fetch on first FAB tap amortizes cost to a single read only when needed.

**Delta-based difficulty change (`+1`/`-1`)** — rejected. A card may be severely mismatched. Storing direction only (`DIFFICULTY_TOO_EASY` / `DIFFICULTY_TOO_HARD`) lets the AI agent assign an appropriate new value rather than nudging incrementally.

**Auto-resume voice after dialog dismiss** — rejected. User paused to read a card; auto-resuming overrides that intent. One manual tap to resume is an acceptable UX cost.

**Empty document when all actions removed** — rejected. Empty curation docs have no meaning and create noise in the sync script's query results. Delete doc on last action removal.

## Consequences

- `users/{uid}/curationRequests/{cardId}` → `{ subcategoryId: String, actions: { "<CurationAction>": { flaggedAt: Timestamp } } }`
- `CurationAction` values: `DIFFICULTY_TOO_EASY`, `DIFFICULTY_TOO_HARD`, `DELETE`, `BACKTICK_REDO`, `NEEDS_CODE_EXAMPLE`, `FULL_REDO`
- `DIFFICULTY_TOO_EASY` and `DIFFICULTY_TOO_HARD` are mutually exclusive — map key semantics enforce this (writing one overwrites the other).
- Curation state is loaded lazily on first FAB tap via a batched `whereIn` query (chunks of 30 due to Firestore limit) and cached in `StudySessionViewModel` for the session.
- Writes are optimistic: local cache updated immediately, Firestore written in background, snackbar error + cache revert on failure.
- FAB and all curation code paths are guarded by `BuildConfig.DEBUG`. Release builds are unaffected.
- `CurationRepository` is a new interface separate from `FlashcardRepository` — different Firestore collection, different domain concern.
- No Flags Screen integration — Curation Requests are invisible to users.
