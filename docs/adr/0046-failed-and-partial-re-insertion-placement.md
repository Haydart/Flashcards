# Failed and Partial cards re-enter the queue at a bounded random distance

## Decision

A Rated Study Session's queue is a mutable list. When a card is rated Failed or Partial and has
Attempts remaining, it is re-inserted at

```
currentIndex + random(MIN_GAP..MAX_GAP)
```

clamped to the end of the queue.

| Rating | Gap |
|---|---|
| Failed | 2–4 cards |
| Partial | 5–9 cards |

Worse recall returns sooner. If the remaining queue is shorter than the drawn gap, the card is
appended at the end.

The random draw is derived from `StudySessionConfig.seed`, the same seed the Preview screen already
uses for card selection, so a session's entire card order is reproducible from its configuration.

A card whose Attempts are exhausted is not re-inserted; it resolves to a Terminal State
([ADR-0044](0044-three-valued-terminal-state.md)). With the "Partial ends the card" setting on, a
Partial Rating resolves immediately and is never re-inserted.

## Context

The domain vocabulary said only that a Partial or Failed Rating "triggers re-insertion of the
Flashcard into the session queue". Where it lands was never specified anywhere, and the runtime has
to choose.

Appending to the end is the obvious implementation and the wrong one: with a session of any length
it means every failed card is re-tested in one clump at the finish, in the order they were failed,
which is both predictable and maximally distant from the moment the user got them wrong.

Unbounded random placement fixes the predictability and introduces a worse failure — it can land the
card at the very next position, so the user re-answers a question whose answer is still on screen.
That is not recall, it is transcription, and it makes the Attempt meaningless.

The gap therefore needs a floor and a ceiling. Making the floor differ by Rating is what turns the
mechanic from shuffling into something with a direction: a card the user failed outright needs
reinforcement sooner than one they half-knew, which is the ordinary spaced-repetition gradient
applied within a single session.

## Alternatives considered

**Append to the end of the queue** — rejected, as above: predictable, clumped, and maximally delayed
from the failure.

**Uniform random across the remaining queue** — rejected. It can re-present the card immediately,
with the answer still visible.

**A fixed gap, the same for both Ratings** — rejected. It is predictable in a way users notice and
can game ("this one always comes back four cards later"), and it throws away the distinction the
Rating just made.

**Failed further away than Partial** — considered and inverted. It reads plausibly — give the user
time to recover before re-testing something they got badly wrong — but it inverts the
spaced-repetition gradient, spending the session's remaining attention on material the user already
half-knows.

**An unseeded random source** — rejected. It makes the queue's behaviour untestable and a reported
session order irreproducible.

## Consequences

- The session's queue is mutable, not a fixed list with an advancing index, and its length grows as
  cards are re-inserted. Any "card N of M" progress display must decide what M means; the honest
  reading is distinct cards, not queue entries.
- The gap constants are tunable and belong with the rest of the session's tunables rather than
  scattered in the state machine.
- Because placement is seeded, the queue's whole evolution is reproducible in tests from a
  configuration alone, without stubbing a random source.
- A card can be re-inserted past the end of the queue when few cards remain, so late-session
  failures naturally cluster toward the end regardless of the gap. This is unavoidable and
  acceptable.
