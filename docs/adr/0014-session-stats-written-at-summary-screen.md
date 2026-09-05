# Session results are committed once, at the Session Summary screen

## Decision

### Nothing is written during a session

No Firestore write occurs while a Study Session runs — not per card, not per Rating, not per voice
grade. The session accumulates its outcome in memory and hands it forward.

### The session record

One collection holds every Study Session, Rated and Fast alike:
`users/{uid}/sessions/{sessionId}`.

```
sessionId: String
startTimestamp: Timestamp
durationSeconds: Int
studyMode: RATED | FAST
isPartial: Boolean
categoryId: String
categoryName: String            // denormalized
subcategoryIds: List<String>
subcategoryNames: List<String>  // denormalized
cardCount: Int
cardsMastered: Int
cardsPartial: Int
cardsDefended: Int
cardsDemastered: Int
newCardsStudied: Int
```

The document is deliberately **slim**: aggregates and denormalized names only. Home's Recents
carousel is the highest-traffic reader of this collection, and it renders `categoryName`,
`subcategoryNames` and `cardCount` and nothing else. Every per-card byte in this document would be
dead weight on one of the app's most-loaded screens.

The names are denormalized so a Recents card renders from a single `orderBy(startTimestamp)
.limit(n)` query with no joins. That query's cost is the limit, not the collection size, so the
collection may grow without bound.

Per-card outcomes go in a subcollection, `users/{uid}/sessions/{sessionId}/outcomes/{cardId}`:

```
cardId: String
terminalState: Mastered | Partial | Failed | Seen
attemptsUsed: Int
wasPreviouslyMastered: Boolean
transcript: String?   // voice-answered cards only
```

It is written at the same moment as its parent, and read only when the user opens a detailed
post-session review of a past session. Home never touches it.

### The commit is one batch, at the Summary screen

The Session Summary screen computes the XP breakdown from the session result and the `XpConfig`
snapshot ([ADR-0047](0047-xp-values-behind-a-config-repository.md)), then performs a **single
batched write**:

- the `sessions/{sessionId}` document
- its `outcomes/{cardId}` documents
- `cardProgress/{cardId}` creates and updates ([ADR-0016](0016-card-progress-model.md))
- `subcategoryProgress/{subcategoryId}` counter deltas
- `users/{uid}` — `xp`, `level`, `xpIntoCurrentLevel`, `currentStreak`, `bestStreak`,
  `lastStudyDate`, `goalMetDate`

One batch, one atomic commit. A session is either fully recorded or not recorded at all.

The Summary screen is the mandatory exit path for every session, partial included: deck end and
exit-confirmation both route to it.

### How the result reaches the Summary screen

`StudySummaryRoute` carries **`sessionId: String` and nothing else**. The session result itself is
handed over through an `@ActivityRetainedScoped` holder, written by the session ViewModel as it
terminates and read once by the Summary ViewModel.

The session ViewModel therefore does not commit. It seals its ledger, stamps `durationSeconds`,
writes the result to the holder, and emits a navigation event.

When the user opens a **past** session's detail view, the same route and screen serve it: the id is
present, the holder is empty, and the Summary reads `sessions/{sessionId}` and its `outcomes`
subcollection instead.

## Context

A 150-card Rated session writing each outcome as it happens is up to 150 Firestore writes, which
does not scale across users. Worse, it has no clean boundary: a user exiting mid-session leaves half
their progress in Firestore with nothing recording that the session was cut short. The session
ViewModel already holds every outcome in memory, so deferring costs nothing.

Deferring raises the question of *how far*. Two candidates: commit when the session terminates, then
show a Summary that reads back what was written; or carry the result to the Summary and commit
there. The XP breakdown decides it. XP is computed at the Summary — it is the screen that animates
it line by line — and whoever computes XP must persist it. Splitting the two means two writes and a
window in which a session document exists with no XP applied to the user.

The result-handoff question is separate, and constrained by the navigation library.
`androidx.navigation` derives a `NavType` only for primitives, enums and lists of primitives, which
this codebase already discovered and worked around by flattening `VoiceSettings` and `IntRange` into
primitive route fields. Carrying a per-card ledger as a route argument would need this repo's first
hand-written `CollectionNavType`, JSON-encoding roughly 9–35 KB into a string that also serves as
the back-stack identity key and the deep-link regex match target. Size is not the problem — that is
orders of magnitude below the Binder ceiling — the route string's other jobs are.

## Alternatives considered

**Incremental per-rating writes** — rejected on write volume and on partial-session ambiguity, as
above.

**Commit at termination; Summary reads it back** — rejected. It splits XP computation from XP
persistence, needs two writes, and leaves a window where a session is recorded but its XP is not.

**Separate `recentSessions` and `sessions` collections**, one denormalized for Home and one detailed
for stats — rejected. Two documents per session that must agree, written from the same batch,
differing only in which fields they carry. One slim document with denormalized names serves both
readers.

**A fat session document carrying the per-card ledger inline** — rejected. Home's Recents pays for a
payload it never renders on every load.

**Custom `CollectionNavType` carrying the full ledger in the route** — rejected. It works and it
sizes fine, but it breaks the flattening convention this codebase already settled on, and it makes a
35 KB back-stack key.

**A bare `data object StudySummaryRoute` with the result taken entirely from the holder** — rejected.
It leaves no way to address a *past* session, so the future detailed-review screen would need a
second route and a second screen for what is the same view over the same data.

## Consequences

- If the app is killed while the Summary screen is showing, the session is lost entirely. This is
  the same exposure as a mid-session crash and is accepted; a future mitigation could persist the
  in-progress ledger to DataStore and recover on next launch.
- `users/{uid}` gains `lastStudyDate` and `goalMetDate`. Streak continuation and the once-per-day
  daily-goal award are both uncomputable without them — the first needs to know whether a session
  today has already been counted, the second whether today's goal was already met.
- Day attribution uses the session's **start** timestamp, not the commit time, so a session that
  crosses midnight counts toward the day it began.
- The Summary ViewModel has two load paths — fresh result from the holder, past session from
  Firestore — and must not commit on the second.
- The holder is `@ActivityRetainedScoped`: it survives configuration change and dies with the nav
  graph. It is cleared once read.
