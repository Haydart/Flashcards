# The card progress model: one `cardProgress` collection, two derived sets, two read levels

## Decision

### One per-card collection

A User's per-card progress lives in `users/{uid}/cardProgress/{cardId}`:

```
cardId: String
subcategoryId: String
categoryId: String
state: Seen | Failed | Partial | Mastered
firstStudiedAt: Timestamp   // write-once
masteredAt: Timestamp?      // set when state becomes Mastered, retained thereafter
```

Two sets are derived from this one collection:

- **Studied** — a document exists. Monotonic: the document is never deleted.
- **Mastered** — `state == Mastered`. Mutable: de-mastery moves `state` down, it does not delete
  the document.

Mastered is therefore a subset of Studied structurally, not by convention.

### What each Study Mode writes

**Rated** sessions write a card's Terminal State ([ADR-0044](0044-three-valued-terminal-state.md))
into `state`, and set `firstStudiedAt` on first contact. A card is Studied once it has completed at
least one Attempt.

**Fast** sessions create a document with `state = Seen` **if and only if none exists**, and never
modify an existing one. A card is Studied once **its answer has been shown** — `VoicePhase.Answer`
entered under read-aloud, `isAnswerRevealed` set under manual advance. Reaching a card's question
and skipping forward marks nothing.

The create-if-absent rule is what makes this safe: a Fast session can never overwrite a judgement a
Rated session made, so re-listening to a mastered card cannot downgrade it.

Fast gains nothing else. No Ratings, no Attempts, no Terminal States, no Persistent Mastery, no
Mastery Defense, no shield icon.

### Mastery Defense stays Rated-only

**Mastery Defense** — re-inserting previously mastered cards to be re-tested, with a bonus for
holding them and a penalty for losing them — applies to Rated sessions exclusively. Fast has no
Rating step, so there is no outcome to defend with.

De-mastery fires on Terminal **Failed** only. A defended card ending Terminal Partial keeps its
mastery and earns neither the bonus nor the penalty.

### Private Flashcards are excluded entirely

A Private Flashcard never receives a `cardProgress` document, never enters Studied or Mastered, and
never earns card-level XP. Session-level XP — time studied, session completion, streak, daily goal —
is unaffected.

The reason is not purity but exploitability: new-card XP on a user-authored card is trivially
farmable. Create fifty private cards, run one Fast session, collect fifty first-seen awards; master
them, collect fifty masteries.

### Two read levels

| Screen | Reads | Cost |
|---|---|---|
| Category Details (a ring per Subcategory) | `users/{uid}/subcategoryProgress/{subcategoryId}` — `{masteredCount, studiedCount, globalCardCount}` | one small doc per Subcategory |
| Subcategory Details (per-card filtering), Preview (defense selection), Study Session (XP accounting) | the `cardProgress` slice for one Subcategory | one query |

Counters are maintained as `FieldValue.increment` deltas inside the single session-commit batch
([ADR-0014](0014-session-stats-written-at-summary-screen.md)). They are never recomputed by scanning
`cardProgress`.

`globalCardCount` lives **on the counter document** rather than being derived. Without it, drawing
one ring needs a second read per Subcategory purely to obtain a denominator, which defeats the
counter document's purpose. It is maintained by the seed tooling, not the app.

## Context

Two questions had to be answered together, because each constrains the other's storage shape.

**What does progress mean for a user who only studies in Fast mode?** Fast has no Rating step: TTS
reads the question, pauses, reads the answer, advances. Inferring mastery from listening is not
meaningful, and a mid-session tap to self-rate while TTS runs breaks the passive model that is the
whole point of the mode. But recording *nothing* means a Fast-only user's progress rings read zero
forever no matter how much they study, and the app gives them no hint that their study mode is the
reason.

The resolution is that **coverage and quality are different claims**. `Seen` is not a weak mastery
signal, it is the absence of a quality judgement — something Fast can assert truthfully and cheaply.
Mastery inference remains off-limits to Fast; coverage does not.

**How are two differently-mutable sets over the same cards stored?** Studied is monotonic, Mastered
is not. Modelling each as its own collection means two sources of truth for one card, an
unenforceable subset invariant, and double the writes.

The read-shape question is forced by the UI rather than the data. Category Details renders a ring
for every Subcategory in a Category — thirteen in the Android case — on a screen users open
constantly. Answering that from per-card documents is hundreds of reads per open. Subcategory
Details needs the opposite: genuine per-card state, to filter by Mastered / Studied / Unseen.
Neither shape serves both screens, so both exist.

## Alternatives considered

**Fast records nothing; Fast-only users get no rings** — rejected. It makes the progress rings
punish a legitimate study mode, silently.

**Fast infers mastery from listening, or offers a mid-session self-rate while TTS runs** — rejected,
and this is the decision that has not changed. The first fabricates a signal; the second destroys
the passive-listening model. Showing mastered cards as visually distinct in Fast without any
interaction model was rejected too — a shield icon with no actionable meaning only confuses.

**Fast marks the whole selected pool as Seen at session start** — rejected. A user exiting after
three cards would bank coverage for a deck they never saw, and Studied would stop meaning anything.

**Fast marks a card Seen when its question appears** — rejected. Fast's transport controls make
skipping cheap and common; a question that flashed past taught nothing. In this mode the answer is
the study event.

**Fast requires the card to be fully completed and advanced past** — rejected as too strict at the
boundary: it silently drops the last card of every session a user exits on, which is the card they
most recently studied.

**Parallel `studiedCards` and `masteredCards` collections** — rejected. Two sets over the same cards
with an invariant nothing enforces.

**Counters only, no per-card documents** — rejected. Draws the rings cheaply but cannot answer "is
this specific card already mastered?", which Mastery Defense selection needs, nor support per-card
filter chips.

**Per-card documents only, no counters** — rejected on read cost at Category Details.

**Deleting a card's document on de-mastery** — rejected. It makes Studied non-monotonic, so a user
who mastered a card and later failed it watches their coverage ring go backwards for a card they
demonstrably studied.

**Including Private Flashcards, excluded from Mastered but counted in the ring denominator** —
rejected. The Mastered ring could then never reach 100% for any Subcategory the user has added a
private card to, showing a permanent ceiling with no visible cause.

## Consequences

- Fast sessions perform per-card Firestore writes, which they previously did not. Volume is bounded
  by genuinely new cards only, so it decays toward zero as coverage grows, and it lands in the
  single commit batch rather than as writes during the session.
- A Fast session's state must track which cards reached the answer — one boolean set it did not
  previously need.
- A Rated session's per-card state must retain the **best Rating so far**; see ADR-0044.
- `CardProgressRepository` exposes both read shapes and serves three screens, so it belongs in
  `core:domain` rather than inside `feature:study`.
- A Subcategory's ring denominator is its **global** card count, not its total card count. The
  card-count label printed beside a ring must use the same figure, or the percentage cannot be
  reconciled against the number next to it.
- A user who studies only Private cards sees rings that never move. Their Progress screen still
  advances on time studied, streak and daily goal.
- The Firestore `whereIn` cap of 30 ids means a 60-card session's progress lookup is either two
  queries or one read of the Subcategory's whole `cardProgress` slice.
- A user who studies exclusively in Fast mode still never gains or loses Persistent Mastery, and
  their XP growth stays slower than a Rated user's — card mastery and defense are the larger awards.
  Only their coverage now advances.
