# Persistent Card Mastery

## Overview

A cross-session record of which Flashcards a User has ever mastered. Enables Mastery Defense mechanics in Rated Study Sessions. Mastery mechanics are **Rated sessions only** — Fast sessions are entirely excluded.

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

**Mastery gained:** A Flashcard reaches a Correct Rating in any Attempt within a Rated Study Session (Terminal State: Mastered). On session end (Session Summary write), the card is added to `masteredCards` if not already present. `masteredAt` is updated if already present.

**Mastery lost (de-mastery):** A previously mastered card reaches a Failed Terminal State in a Rated Study Session (3 Attempts, no Correct Rating). On session end, the card document is deleted from `masteredCards`.

**Mastery defended:** A previously mastered card receives a Correct Rating on any of its 3 Attempts in a Rated session. Mastery is retained in `masteredCards`; bonus XP awarded (see [XP & Leveling System](xp-leveling-system.md)).

## Mastery Defense insertion

Previously mastered cards are re-inserted into the session's Flashcard pool at Pre-start Screen time (Pre-start Screen owns all card selection — ADR-0004).

Rules:
- Only cards within the session's Category/Subcategory scope are eligible
- Up to **10% of the selected card pool** is filled with mastered cards (rounded; minimum 0)
- Mastered cards are inserted in addition to the normal pool, then the combined pool is randomized
- The count of mastery defense cards is **not shown** on the Pre-start Screen — internal mechanic, transparent to the user

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
