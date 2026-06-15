# Flag system: two-action signal, no suppression, mutable per-card, subcategoryId denormalized on flag doc

## Decision

Users can raise a **Flag** on any global Flashcard. A Flag carries one of two **Flag Actions**: **Retire** (card should be deleted — too obscure or irrelevant) or **Rework** (card should be edited — imprecise or poorly worded). Flags are stored at `users/{uid}/flaggedCards/{cardId}` with fields `{ action, flaggedAt, subcategoryId }`. One Flag per card per user — upsert on `cardId` overwrites the previous flag. Flagged Flashcards are **not suppressed** from Study Sessions. Flag management (change action, withdraw) is done from the Flags Screen, accessible via Settings. `subcategoryId` is stored redundantly on the Flag document despite ADR-0007 excluding it from Flashcard documents.

## Context

The global Flashcard pool is large and AI-generated. Users encounter cards that are too obscure to be worth studying or too imprecisely worded to be reliable. There is no practical way to curate them all at creation time. The feature provides a lightweight in-session or in-browse signal that queues cards for admin review without disrupting the study flow.

## Alternatives considered

**Single flag with no action distinction** — rejected. The admin triage action differs fundamentally: Retire requires deletion, Rework requires editing. Losing that signal forces the admin to re-evaluate each card from scratch.

**Personal suppression (hide from sessions)** — rejected. Suppression adds local state that can diverge from admin action. If an admin reworks a card, the suppressed user never sees the improvement. Keeps the system simpler: one source of truth (Firestore global pool).

**Immutable append-log per card** (`users/{uid}/flaggedCards/{cardId}/events/{id}`) — rejected. Complicates admin triage (must deduplicate per user per card to find current intent). Upsert on `cardId` gives a single current-intent record with no extra reads.

**Suppress on Retire, not on Rework** — rejected. Forces the user to predict admin outcome mid-session. Both action values suppress equally well: a Rework card is still broken until the admin edits it.

**Not storing `subcategoryId` on the Flag doc** — rejected. The Flags Screen groups flags by Subcategory; rendering requires subcategoryId. Reconstructing it from `cardId` alone is not possible (cardId carries no encoded subcategoryId). A separate lookup per flag would require N reads for N flags — unacceptable for a list screen. Denormalization matches the existing pattern established for `recentSessions` (ADR-0007 note: the exclusion of `subcategoryId` from *Flashcard* documents is path-encoding — Flag documents are a different entity with different read patterns).

## Consequences

- `users/{uid}/flaggedCards/{cardId}` → `{ action: "RETIRE"|"REWORK", flaggedAt: Timestamp, subcategoryId: String }`
- `cardId` is globally unique (guaranteed by seed tooling), so a flat `flaggedCards/` collection with cardId as key has no collision risk.
- Flags Screen fetches `users/{uid}/flaggedCards` in a single `getDocuments()` — no index needed. Groups client-side by `subcategoryId`.
- To render a flagged card's question/answer, the app fetches `subcategories/{subcategoryId}/flashcards/{cardId}` — one read per flag (or batched). Acceptable for a management screen with typically low flag count.
- Admin triage queries across users: `collectionGroup("flaggedCards").where("action", "==", "RETIRE")` — requires a Firestore composite index on `action`.
- `cardCount` on Subcategory docs is unaffected — flagging does not change the global pool.
