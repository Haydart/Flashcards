# Session results are committed once, at the Session Summary screen

## Decision

### Nothing is written during a session

No Firestore write occurs while a Study Session runs — not per card, not per Rating, not per voice
grade. The session accumulates its outcome in memory and hands it forward.

### The session record carries its own ledger

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
outcomes: {                     // keyed by cardId
  <cardId>: {
    subcategoryId: String
    state: Mastered | Partial | Failed | Seen
    attemptsUsed: Int
    wasPreviouslyMastered: Boolean
    transcript: String?         // voice-answered cards only
  }
}
```

**One session is one document.** The per-card ledger is embedded, not held in a subcollection.

The names are denormalized so a Recents card renders from a single `orderBy(startTimestamp,
DESCENDING).limit(n)` query with no joins. That query's cost is the limit, not the collection size,
so the collection may grow without bound.

The per-outcome counts are stored alongside the ledger rather than derived from it on read, so a
session's scoring breakdown is reproducible from the record without walking every entry.

### Why the ledger is embedded

Firestore bills **per document read**, not per byte. A Recents carousel showing ten sessions costs
ten reads whether those documents are slim or fat, so splitting the ledger into a subcollection buys
no read saving at all — it only halves the bytes on the wire, at the cost of doubling the writes on
every commit and adding a second fetch whenever a past session is opened.

Size is bounded by construction: one entry per distinct card, and a session's length is capped at
`StudySessionConfig.MAX_LENGTH`. The worst case is a 50-card voice-answered session carrying 50
transcripts, roughly 13 KB against Firestore's 1 MiB document limit.

The one real cost is that the Android client SDK has no field projection, so Recents transfers a
ledger it never renders. If that ever measures badly, the fix is a separate slim index document —
an optimisation to make when it is needed, not a reason to pay two writes per session forever.

### The commit is one batch, at the Summary screen

The Session Summary screen computes the XP breakdown from the session result and the `XpConfig`
snapshot ([ADR-0047](0047-xp-values-behind-a-config-repository.md)), then performs a **single
batched write**:

- the `sessions/{sessionId}` document, ledger included
- `progress/{subcategoryId}` — one packed progress document per Subcategory touched
  ([ADR-0016](0016-card-progress-model.md))
- `users/{uid}/state/progressSummary` — nested-key counter increments
- `users/{uid}/state/progression` — `xp`, `level`, `xpIntoCurrentLevel`, `currentStreak`,
  `bestStreak`, `lastStudyDate`, `goalMetDate`

**A single-Subcategory session therefore commits four writes**, whatever its length: session,
progress, summary, progression. A composite session commits one further `progress` write per
additional Subcategory touched — three fixed writes plus one per Subcategory.

**This batch is atomic but not transactional.** It commits or fails as a unit, but it does not
re-read `progress` or `state/progression` at commit time, so two sessions racing on the same
Subcategory can both read the same starting state and both apply their increments, double-counting
`studiedCount`, mastery deltas and XP. This is an accepted limitation for a single-account project —
a transactional, idempotency-checked commit is future work if genuine multi-device concurrency ever
needs to be supported.

### Per-User singletons live in a `state` collection

Both the progress summary and the scoring state are one document per User. Firestore paths alternate
collection and document, so each needs a fixed document id inside a collection:

```
users/{uid}/state/progressSummary
users/{uid}/state/progression
```

One security rule covers the collection, and a future singleton needs no new rule.

**Scoring state does not live on `users/{uid}` itself.** Entitlement is not a field on that
document — it is the separate subcollection `users/{uid}/entitlement/premium`
(`functions/src/lib/entitlement.ts`), written only by the Admin SDK and read server-side by the
premium Cloud Function; that subcollection stays default-denied regardless of any rule granted on
the parent document, so making `users/{uid}` client-writable would not by itself expose it. The real
reason is simpler separation of concerns: `users/{uid}` is reserved for identity and admin-managed
data, and a session commit should touch exactly the documents scoring needs and nothing that isn't
scoring. A separate client-owned document under `state/` keeps that boundary clean and costs the
same single write.

The summary and the scoring state stay **two** documents rather than one. The summary is a maintained
rollup that may need self-healing by recounting the packed progress documents and overwriting; the
scoring state is authoritative and derivable from nothing. Merging them would let a self-heal path
clobber a User's XP. One batch, one atomic commit. A session is either fully recorded or not
recorded at all.

The Summary screen is the mandatory exit path for every session, partial included: deck end and
exit-confirmation both route to it.

### How the result reaches the Summary screen

The Summary route carries the whole session result as route arguments, flattened into primitives
and lists of primitives the same way `StudySessionRoute` already flattens `VoiceSettings` and
`IntRange` — `androidx.navigation`'s typesafe routes only derive a `NavType` for primitives, enums
and lists of those. The per-card ledger becomes one parallel list per field (`cardIds`,
`subcategoryIds`, `states`, `attemptsUsed`, `wasPreviouslyMastered`, `transcripts`), all indexed
together. A **past** session's detail view instead carries only `sessionId`, and the Summary reads
`sessions/{sessionId}` back from Firestore — one document, everything included — rather than
receiving a ledger through the route.

The session ViewModel therefore does not commit. It seals its ledger, stamps `durationSeconds`, and
navigates to the Summary with the flattened result as route arguments.

## Context

A full-length Rated session writing each outcome as it happens is up to `StudySessionConfig.MAX_LENGTH`
(50) Firestore writes, which does not scale across users. Worse, it has no clean boundary: a user
exiting mid-session leaves half their progress in Firestore with nothing recording that the session
was cut short. The session ViewModel already holds every outcome in memory, so deferring costs
nothing.

Deferring raises the question of *how far*. Two candidates: commit when the session terminates, then
show a Summary that reads back what was written; or carry the result to the Summary and commit
there. The XP breakdown decides it. XP is computed at the Summary — it is the screen that animates
it line by line — and whoever computes XP must persist it. Splitting the two means two writes and a
window in which a session document exists with no XP applied to the user.

It also raises *how many documents*. The original shape here — a slim parent plus a per-card
`outcomes` subcollection, alongside a per-card progress collection — cost around a hundred writes
for a full session. Since Firestore's billed unit is the operation, that number is the one that has
to come down, and both halves of it come down by packing: the ledger into its session document, and
card progress into one document per Subcategory.

The result-handoff question is separate, and constrained by the navigation library.
`androidx.navigation` derives a `NavType` only for primitives, enums and lists of primitives, which
this codebase already discovered and worked around by flattening `VoiceSettings` and `IntRange` into
primitive route fields. The per-card ledger follows the same convention: one parallel list per
field, all indexed together, rather than one JSON blob. A session is capped at
`StudySessionConfig.MAX_LENGTH` (50 cards), so the flattened lists stay small — nowhere near the
route string's practical size limits — and the route needs no custom `NavType`.

## Alternatives considered

**Incremental per-rating writes** — rejected on write volume and on partial-session ambiguity, as
above.

**Commit at termination; Summary reads it back** — rejected. It splits XP computation from XP
persistence, needs two writes, and leaves a window where a session is recorded but its XP is not.

**A per-card `outcomes` subcollection under the session** — rejected on operation cost. It was the
original decision here, justified by keeping the Recents carousel's reads small. That justification
does not survive scrutiny: reads are billed per document, so Recents pays the same ten reads either
way. The subcollection only ever saved bandwidth, and it charged a write per card to do it.

**A single `outcomes` detail document as a child of the session** — rejected. It keeps Recents slim
and bounds the write count at two, which is defensible, but it still costs one extra write per
session and one extra read whenever a past session is opened, to save bytes on a screen whose reads
are already cached.

**Separate `recentSessions` and `sessions` collections**, one denormalized for Home and one detailed
for stats — rejected. Two documents per session that must agree, written from the same batch,
differing only in which fields they carry.

**An `@ActivityRetainedScoped` holder, written by the session ViewModel and read once by the Summary
ViewModel** — rejected. It keeps the route to a bare `sessionId`, but it makes the Summary
ViewModel's state depend on a side channel outside the navigation contract, and its retained scope
outlives what the Summary screen actually needs (it survives configuration change for as long as the
hosting Activity does, not just for the Summary's lifetime). Passing the result as route arguments
keeps state visible in the one place — the back stack — that already has to represent it.

**Custom `CollectionNavType` carrying the full ledger as one JSON blob in the route** — rejected in
favor of flattening. It works and sizes fine, but it breaks the flattening convention this codebase
already settled on for `VoiceSettings` and `IntRange`, trading one custom serializer for what
parallel primitive lists already do natively.

**A bare `data object StudySummaryRoute` with the fresh result read from some other source** —
rejected. It leaves no way to address a *past* session with the same route, so the future
detailed-review screen would need a second route and a second screen for what is the same view over
the same data.

## Consequences

- A session commit is a small, bounded number of writes — four for the common single-Subcategory
  case — independent of how many cards were studied.
- Opening a past session's detail costs **one** read: the session document carries its own ledger.
- Home's Recents transfers ledger data it does not render. Bounded at roughly 13 KB per session and
  served from Firestore's on-device cache after first load; revisit only if measured.
- If the app is killed while the Summary screen is showing, the session is lost entirely. This is
  the same exposure as a mid-session crash and is accepted; a future mitigation could persist the
  in-progress ledger to DataStore and recover on next launch.
- `users/{uid}/state/progression` carries `lastStudyDate` and `goalMetDate`. Streak continuation and
  the once-per-day daily-goal award are both uncomputable without them — the first needs to know
  whether a session today has already been counted, the second whether today's goal was already met.
  Both are local calendar dates stored as `yyyy-MM-dd` strings rather than Timestamps: they are
  calendar days, not instants, and the only questions asked of them are same-day and later-day.
- `users/{uid}` stays admin-only, unreadable and unwritable by the client, exactly as it is today.
- Day attribution uses the session's **start** timestamp, not the commit time, so a session that
  crosses midnight counts toward the day it began.
- The Summary ViewModel has two load paths — fresh result from route arguments, past session from
  Firestore — and must not commit on the second.
- The route arguments carry the whole fresh-result payload, so the Summary needs nothing beyond what
  navigation already hands it, and a process death that survives via `SavedStateHandle` restores the
  same arguments rather than losing the result.
