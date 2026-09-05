# Card Progress and Persistent Mastery

**Status:** Design only — not yet implemented. No `cardProgress`/`subcategoryProgress` writes, Session Summary screen, or session-stats persistence exist in code yet. This doc is written at implementation-ready detail so it can be built directly from.

## Overview

A cross-session record of what a User has done with each Flashcard. It carries two derived sets:

- **Studied** — every Flashcard the User has ever had in front of them. Monotonic.
- **Persistent Mastery** — the Flashcards they currently hold mastery on. Mutable.

Mastered is a subset of Studied by construction. Both are read from one collection, and both drive the progress rings on Category Details and Subcategory Details.

**Both Study Modes contribute to Studied.** A Rated Study Session records a Flashcard's Terminal State; a Fast Study Session records only that the Flashcard was seen. Mastery itself — gaining it, defending it, losing it — remains Rated-only. See [ADR-0016](../adr/0016-card-progress-model.md).

Two Firestore collections work together: `cardProgress` (per-card truth) and `subcategoryProgress` (per-subcategory rollup counters, denormalized for cheap reads — see [Progress rollup](#progress-rollup-subcategoryprogress)).

## Firestore structure

```
users/{uid}/cardProgress/{cardId}
```

Document fields:
- `cardId`: matches the Flashcard document ID
- `subcategoryId`: denormalized for efficient scope queries
- `categoryId`: denormalized for efficient scope queries
- `state`: `Seen` | `Failed` | `Partial` | `Mastered`
- `firstStudiedAt`: timestamp, write-once
- `masteredAt`: timestamp of most recent mastery, null if never mastered, retained after de-mastery

A document exists iff the Flashcard is **Studied**. The Flashcard is in **Persistent Mastery** iff `state == Mastered`. De-mastery moves `state` down; it never deletes the document, so coverage never regresses.

**Private Flashcards are excluded entirely** — they never receive a document, never count toward either set, and never earn card-level XP. New-card XP on a user-authored Flashcard would otherwise be trivially farmable.

## Lifecycle

**First studied (either mode).** A Flashcard the User has no document for gets one created, with `firstStudiedAt` set and `state` set to the session's outcome — the Terminal State in a Rated session, `Seen` in a Fast session. New-card XP is awarded once, here, for either mode.

A Rated Flashcard counts as studied once it has completed at least one Attempt. A Fast Flashcard counts once its **answer has been shown** — `VoicePhase.Answer` entered under read-aloud, `isAnswerRevealed` set under manual advance. Skipping past a question marks nothing.

**Mastery gained.** A Flashcard reaches a Correct Rating on any Attempt in a Rated Study Session (Terminal State: Mastered). On session commit, `state` becomes `Mastered` and `masteredAt` is set. `subcategoryProgress.masteredCount` increments by 1 in the same batch.

**Mastery lost (de-mastery).** A previously mastered Flashcard reaches a **Failed** Terminal State in a Rated Study Session. On session commit, `state` moves to `Failed`. `subcategoryProgress.masteredCount` decrements by 1 in the same batch. The document survives, so `studiedCount` is unaffected.

**Mastery defended.** A previously mastered Flashcard receives a Correct Rating on any Attempt. Mastery is retained; bonus XP awarded (see [XP & Leveling System](xp-leveling-system.md)). No counter write occurs — state doesn't change, only XP does.

**Partial is mastery-neutral.** A previously mastered Flashcard ending on a **Partial** Terminal State keeps `state == Mastered`. It earns neither the defense bonus nor the de-mastery penalty, and writes no counter delta. Only an outright Failed de-masters.

**Fast never downgrades.** A Fast session writes `Seen` **only when no document exists**. Re-listening to a mastered Flashcard cannot move it backwards.

## Progress rollup: `subcategoryProgress`

`cardProgress` is per-card truth, but Category Details needs a **per-subcategory percentage** for every Subcategory it lists at once — thirteen rings for Android. Deriving that from `cardProgress` on every screen visit means N Firestore operations per visit, per User, every time they browse. Instead, maintain running counters updated only at the one write point that already exists (session commit), so reads become a single query.

```
users/{uid}/subcategoryProgress/{subcategoryId}
```

Document fields:
- `subcategoryId`: matches the Subcategory document ID
- `categoryId`: denormalized, so all topics in one Category can be fetched in a single query
- `masteredCount`: Int, currently-mastered Flashcards in this Subcategory
- `studiedCount`: Int, Flashcards with any progress in this Subcategory
- `globalCardCount`: Int, the Subcategory's global (non-Private) Flashcard count — the ring denominator

Both ring percentages are `count / globalCardCount`.

`globalCardCount` lives on this document rather than being read from the `subcategories` doc, so drawing a ring needs no second read per Subcategory. It is maintained by the seed tooling, not the app.

**The denominator excludes Private Flashcards, so the card count printed beside a ring must be the same figure.** A label counting Private Flashcards next to a ring that doesn't produces a percentage the user cannot reconcile against the number next to it.

### Write mechanics

Updated in the **same batch** as the `cardProgress` writes at session-commit time — never separately, so it can't drift from the source it's derived from. While the commit walks each Flashcard's transition it accumulates, per `subcategoryId` touched:

- `masteredDelta`: +1 per newly mastered, −1 per de-mastered, 0 for defended / partial / unchanged
- `studiedDelta`: +1 per Flashcard that had no document before, 0 otherwise

One `subcategoryProgress` write per touched Subcategory joins the batch — in practice one, since a session is usually scoped to a single Subcategory.

```kotlin
deltasBySubcategory.forEach { (subcategoryId, delta) ->
    if (delta.masteredDelta == 0 && delta.studiedDelta == 0) return@forEach
    batch.set(
        db.collection("users").document(uid)
            .collection("subcategoryProgress").document(subcategoryId),
        mapOf(
            "subcategoryId" to subcategoryId,
            "categoryId" to categoryId, // known from session scope
            "masteredCount" to FieldValue.increment(delta.masteredDelta.toLong()),
            "studiedCount" to FieldValue.increment(delta.studiedDelta.toLong()),
        ),
        SetOptions.merge(),
    )
}
```

`FieldValue.increment()` on a missing field/doc starts from 0 and creates the doc via `merge` — no init step for a User's first studied Flashcard in a Subcategory.

### Reading progress

- **Category Details** (all topics in one Category, rings for each): `users/{uid}/subcategoryProgress where categoryId == "android"` — one query, ≤ topic-count reads, one round trip regardless of how much was ever studied. Both ring perspectives render from the same result.
- **Subcategory Details** (per-card filtering by Mastered / Studied / Unseen): the `cardProgress` slice for that one Subcategory — one query. This is the only screen that needs per-card state for display.
- **Preview Study Session Screen**: reads the same slice to pick Mastery Defense candidates.
- **Study Session**: reads the slice for its own card set, to know which Flashcards are new (for new-card XP) and which were previously mastered (for defense accounting). Firestore's `whereIn` caps at 30 ids, so a 60-card session is either two queries or one read of the whole Subcategory slice.
- **Search results** (topics matched across Categories): `where subcategoryId in [matchedIds]` — one query for the matched set.
- **Browse default list**: no progress read — it shows a topic *count* per Category, not per-topic percentages.

`shards` subcollections (ADR-0037) are never touched by any progress read.

### Consistency

The counters are written atomically with `cardProgress` in the same batch, and Firestore batches are all-or-nothing, so they shouldn't drift from normal app usage. Drift remains possible out-of-band: a manual console edit, a future migration, a bug. No proactive reconciliation for v1; if drift is suspected, self-heal by recomputing both counts via `count()` aggregations over the Subcategory's `cardProgress` slice and overwriting the rollup doc — cheap to run occasionally, since it's exactly the aggregation this design avoids running on every screen visit.

## Mastery Defense insertion

Previously mastered Flashcards are re-inserted into the session's pool at Preview Study Session Screen time (Preview owns all card selection — ADR-0004).

Rules:
- Only Flashcards within the session's Category/Subcategory scope are eligible
- Only global Flashcards — Private Flashcards are never defense candidates
- Up to **10% of the session's configured Length** is given over to mastered Flashcards (rounded; minimum 0)
- Defense Flashcards come **out of** the Length budget, not on top of it: a session sized at 20 is 20 Flashcards, 18 new plus 2 defense. The card count and estimated duration on the Preview screen therefore stay truthful
- The combined pool is then randomized
- The count of defense Flashcards is **not shown** on the Preview screen — internal mechanic, transparent to the user

## Visual distinction in session

During a Rated Study Session, defense Flashcards are marked with a **small shield icon** alongside the question. Final visual treatment determined at UI implementation time.

The shield is the only signal — no label, no count, no explanation in the session UI. The XP breakdown on the Session Summary screen implicitly confirms defense was in play.

## Fast mode scope

Fast Study Sessions:
- Write `cardProgress` documents with `state = Seen`, and only where none exists
- Contribute to **Studied** and to `studiedCount`
- Award new-card XP for Flashcards seen for the first time
- Do **not** insert defense candidates, display any mastery distinction, or change `state` on an existing document
- Cannot gain or lose mastery

Mastery state is frozen during Fast sessions; coverage is not.
