# Persistent Card Mastery

**Status:** Design only — not yet implemented. No `masteredCards`/`subcategoryProgress` writes, Session Summary screen, or session-stats persistence exist in code yet (checked `feature/study/.../session/` and repo-wide for `masteredCards`/`sessions` writes, 2026-08-20). This doc is written at implementation-ready detail so it can be built directly from.

## Overview

A cross-session record of which Flashcards a User has ever mastered. Enables Mastery Defense mechanics in Rated Study Sessions. Mastery mechanics are **Rated sessions only** — Fast sessions are entirely excluded.

Two Firestore collections work together: `masteredCards` (per-card truth) and `subcategoryProgress` (per-subcategory rollup counter, denormalized from `masteredCards` for cheap reads — see [Progress rollup](#progress-rollup-subcategoryprogress) below).

## Firestore structure

```
users/{uid}/masteredCards/{cardId}
```

Document fields:
- `cardId`: matches the Flashcard document ID
- `subcategoryId`: denormalized for efficient scope queries
- `categoryId`: denormalized for efficient scope queries
- `masteredAt`: timestamp of most recent mastery

A card exists in this collection iff the User currently holds mastery of it. De-mastery removes the document.

## Lifecycle

**Mastery gained:** A Flashcard reaches a Correct Rating in any Attempt within a Rated Study Session (Terminal State: Mastered). On session end (Session Summary write), the card is added to `masteredCards` if not already present. `masteredAt` is updated if already present. `subcategoryProgress.masteredCount` is incremented by 1 for that card's subcategory, in the same batch.

**Mastery lost (de-mastery):** A previously mastered card reaches a Failed Terminal State in a Rated Study Session (3 Attempts, no Correct Rating). On session end, the card document is deleted from `masteredCards`. `subcategoryProgress.masteredCount` is decremented by 1 for that card's subcategory, in the same batch.

**Mastery defended:** A previously mastered card receives a Correct Rating on any of its 3 Attempts in a Rated session. Mastery is retained in `masteredCards`; bonus XP awarded (see [XP & Leveling System](xp-leveling-system.md)). No `masteredCards` or `subcategoryProgress` write occurs — state doesn't change, only XP does.

## Progress rollup: `subcategoryProgress`

`masteredCards` is per-card truth, but the Study tab search results and Category Details screen need a **per-subcategory mastery percentage** for a User (e.g. "58%" on the Compose topic row). Deriving that from `masteredCards` on every screen visit — either reading every doc and counting client-side, or running a `count()` aggregation query per subcategory shown — means N Firestore operations per screen visit, multiplied by every User, every time they browse. Instead, maintain a running counter that's updated only at the one write point that already exists (Session Summary), so reads become a single query.

```
users/{uid}/subcategoryProgress/{subcategoryId}
```

Document fields:
- `subcategoryId`: matches the Subcategory document ID
- `categoryId`: denormalized, so all topics in one Category can be fetched in a single query
- `masteredCount`: Int, running count of this User's currently-mastered cards in this Subcategory

Percentage shown in the UI = `masteredCount / subcategory.cardCount`. `cardCount` is already denormalized on the `subcategories` doc (ADR-0007) and cached client-side from the Study tab's live listener (see [Reading progress](#reading-progress)), so no extra read is needed for the denominator.

### Write mechanics

Updated in the **same batch/transaction** as the `masteredCards` add/delete at Session Summary time — never as a separate write, so it can't drift out of sync with the source of truth it's derived from. While the Session Summary write logic iterates each card's mastery transition (gained/lost/defended/unchanged) to decide the `masteredCards` change, it also accumulates a `delta` per `subcategoryId` touched in the session (+1 per newly-mastered card, -1 per de-mastered card, 0 for defended/unchanged). One `subcategoryProgress` write per touched subcategory is then added to the batch — in practice almost always 1, since a session is normally scoped to one Subcategory:

```kotlin
val deltasBySubcategory: Map<String, Int> = /* accumulated while walking mastery transitions */

deltasBySubcategory.forEach { (subcategoryId, delta) ->
    if (delta == 0) return@forEach
    batch.set(
        db.collection("users").document(uid)
            .collection("subcategoryProgress").document(subcategoryId),
        mapOf(
            "subcategoryId" to subcategoryId,
            "categoryId" to categoryId, // known from session scope
            "masteredCount" to FieldValue.increment(delta.toLong()),
        ),
        SetOptions.merge(),
    )
}
```

`FieldValue.increment()` on a missing field/doc starts from 0 and creates the doc via `merge` — no separate init step needed for a User's first mastered card in a Subcategory.

### Reading progress

- **Category Details screen** (all topics in one Category): `users/{uid}/subcategoryProgress where categoryId == "android"` — one query, ≤ topic-count reads (13 for Android), one round trip regardless of how many cards were ever mastered.
- **Search results** (topics matched across Categories): `where subcategoryId in [matchedIds]` (Firestore `in` caps at 30 values) — one query for the small set of matched topics.
- **Study tab default list**: no progress read — that screen only shows a topic *count* per Category ("13 topics"), not per-topic percentages.

`flashcards` subcollections are never touched by any progress read — the count lives entirely in `subcategoryProgress`, derived once at write time.

### Consistency

Because the counter is written atomically with `masteredCards` in the same batch, it can only drift if that batch partially fails in a way Firestore's batched-write semantics don't already guard against (batches are all-or-nothing) — so in practice it shouldn't drift from normal app usage. Drift is still possible from out-of-band causes: a manual Firestore console edit to either collection, or a future migration/bug. No proactive reconciliation is planned for v1; if drift is ever suspected, self-heal by recomputing `masteredCount` via a `count()` aggregation on `masteredCards where subcategoryId == X` and overwriting the rollup doc — cheap to run occasionally (e.g. lazily on Progress screen load) since it's the same aggregation this design avoids running on every screen visit.

## Mastery Defense insertion

Previously mastered cards are re-inserted into the session's Flashcard pool at Preview Study Session Screen time (Preview Study Session Screen owns all card selection — ADR-0004).

Rules:
- Only cards within the session's Category/Subcategory scope are eligible
- Up to **10% of the selected card pool** is filled with mastered cards (rounded; minimum 0)
- Mastered cards are inserted in addition to the normal pool, then the combined pool is randomized
- The count of mastery defense cards is **not shown** on the Preview Study Session Screen — internal mechanic, transparent to the user

## Visual distinction in session

During a Rated Study Session, mastery defense cards are visually marked with a **small shield icon** displayed alongside the question. Final visual treatment (color, placement, size) determined at UI implementation time.

The shield is the only signal — no label, no count, no explanation surfaced in the session UI. The XP breakdown on the Session Summary screen implicitly confirms that mastery defense was in play.

## Fast mode exclusion

Fast Study Sessions:
- Do not insert mastered cards as mastery defense candidates
- Do not display any mastery visual distinction
- Do not write to or read from `masteredCards`
- Cannot gain or lose mastery

Mastery state is fully frozen during Fast sessions.

## Private Flashcards

Private Flashcards are excluded from mastery tracking. `masteredCards` only contains global admin-curated Flashcard IDs.
