# Session results are committed once, at the Session Summary screen

## Decision

### Nothing is written during a session

No Firestore write occurs while a Study Session runs — not per card, not per Rating, not per voice
grade. The session accumulates its outcome in memory and hands it forward.

### The session record carries its own ledger

One collection holds every Study Session, Rated and Fast alike:
`users/{uid}/sessions/{sessionId}`.

```text
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
`StudySessionConfig.MAX_LENGTH`. Even a full 50-card session is a handful of scalar fields per entry,
nowhere near Firestore's 1 MiB document limit.

**No transcript is persisted, anywhere.** A voice-answered card's sanitized transcript is surfaced
only transiently, on screen during the Rated session itself, to show the user what was heard before
grading it. Once the card is graded, only the resulting `state` and `attemptsUsed` carry forward into
the ledger — the spoken content itself is not retained in Firestore, in the session ViewModel's
result, or on the Summary route. Nothing today reads a transcript back after the session ends; adding
persistence for a hypothetical future revisit feature is deferred until that feature is actually
designed (see `docs/design/premium-voice-grading-pipeline.md`).

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

**The commit is guarded against being applied twice for the same session.** Unlike the
cross-session race above, a duplicate commit of the *same* `sessionId` — a retried write after a
dropped response, say — is straightforward to prevent: the batch includes a create-only write
(`create()`, which fails if the document already exists) for `sessions/{sessionId}` itself. If that
document already exists, the whole batch fails and nothing is double-applied. This needs no
transaction, only that the session document's write in the batch uses `create` rather than `set`.

### Per-User singletons live in a `state` collection

Both the progress summary and the scoring state are one document per User. Firestore paths alternate
collection and document, so each needs a fixed document id inside a collection:

```text
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
`subcategoryIds`, `states`, `attemptsUsed`, `wasPreviouslyMastered`), all indexed together — no
transcript field, since none is persisted (see above).

**This route is fresh-session egress only.** It is the mandatory exit for both Study Modes, natural
end or premature exit, and nothing else — it is never used to view a past session. A past session's
detail view, if it is ever built, is a wholly separate screen and route reading
`sessions/{sessionId}` back from Firestore directly; it does not share this route, and this route's
shape is not constrained by that hypothetical future screen at all.

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
rejected for the same reason as the `@ActivityRetainedScoped` holder above: the fresh result would
still need a side channel outside the navigation contract to reach the screen, just under a different
name. *(An earlier version of this rationale also argued this shape blocks a future past-session
detail view from sharing the same route. That turned out not to be the plan: the Summary route is
fresh-session egress only, and a past-session detail view, if built, is a separate screen and route
entirely — see "How the result reaches the Summary screen" above. The route-shape decision itself is
unchanged; only this piece of the reasoning for it was corrected.)*

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
- The Summary ViewModel has exactly **one** load path — the fresh result from route arguments. It is
  never used to view a past session; a past session's detail view, if built later, is a separate
  screen and route, reading `sessions/{sessionId}` back from Firestore on its own, not through this
  ViewModel.
- The route arguments carry the whole fresh-result payload, so the Summary needs nothing beyond what
  navigation already hands it, and a process death that survives via `SavedStateHandle` restores the
  same arguments rather than losing the result. Nothing sensitive rides in that payload: no transcript
  is ever part of the result, so `SavedStateHandle`'s disk-backed persistence carries only card ids,
  states and counts.
