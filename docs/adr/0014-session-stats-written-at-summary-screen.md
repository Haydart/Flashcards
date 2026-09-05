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

The names are denormalized so a Recents card renders from a single `orderBy(startTimestamp)
.limit(n)` query with no joins. That query's cost is the limit, not the collection size, so the
collection may grow without bound.

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
progress, summary, progression.

### Per-User singletons live in a `state` collection

Both the progress summary and the scoring state are one document per User. Firestore paths alternate
collection and document, so each needs a fixed document id inside a collection:

```
users/{uid}/state/progressSummary
users/{uid}/state/progression
```

One security rule covers the collection, and a future singleton needs no new rule.

**Scoring state must not live on `users/{uid}` itself.** That document carries `entitlement`, which
only the Admin SDK may write and which the premium Cloud Function reads server-side; it has no client
rule at all, in either direction. Allowing the client to write it so the Summary can save XP is a
privilege escalation — a user writes their own entitlement and the Function reads the doctored field.
A field-level rule excluding `entitlement` would work, but it puts one subtle expression between a
user and free premium, and every future admin-only field has to be remembered into it. A separate
client-owned document has no such failure mode and costs the same single write.

The summary and the scoring state stay **two** documents rather than one. The summary is a maintained
rollup that may need self-healing by recounting the packed progress documents and overwriting; the
scoring state is authoritative and derivable from nothing. Merging them would let a self-heal path
clobber a User's XP. One batch, one atomic commit. A session is either fully recorded or not
recorded at all.

The Summary screen is the mandatory exit path for every session, partial included: deck end and
exit-confirmation both route to it.

### How the result reaches the Summary screen

`StudySummaryRoute` carries **`sessionId: String` and nothing else**. The session result itself is
handed over through an `@ActivityRetainedScoped` holder, written by the session ViewModel as it
terminates and read once by the Summary ViewModel.

The session ViewModel therefore does not commit. It seals its ledger, stamps `durationSeconds`,
writes the result to the holder, and emits a navigation event.

When the user opens a **past** session's detail view, the same route and screen serve it: the id is
present, the holder is empty, and the Summary reads `sessions/{sessionId}` — one document,
everything included.

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

It also raises *how many documents*. The original shape here — a slim parent plus a per-card
`outcomes` subcollection, alongside a per-card progress collection — cost around a hundred writes
for a full session. Since Firestore's billed unit is the operation, that number is the one that has
to come down, and both halves of it come down by packing: the ledger into its session document, and
card progress into one document per Subcategory.

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

**Custom `CollectionNavType` carrying the full ledger in the route** — rejected. It works and it
sizes fine, but it breaks the flattening convention this codebase already settled on, and it makes a
35 KB back-stack key.

**A bare `data object StudySummaryRoute` with the result taken entirely from the holder** — rejected.
It leaves no way to address a *past* session, so the future detailed-review screen would need a
second route and a second screen for what is the same view over the same data.

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
- The Summary ViewModel has two load paths — fresh result from the holder, past session from
  Firestore — and must not commit on the second.
- The holder is `@ActivityRetainedScoped`: it survives configuration change and dies with the nav
  graph. It is cleared once read.
