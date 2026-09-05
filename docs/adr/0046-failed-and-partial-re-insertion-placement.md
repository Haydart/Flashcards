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

The draw uses the standard library's random generator. There is no session seed anywhere in the app:
card selection and queue evolution both randomize freely, and neither is reproducible from a session's
configuration. A predictable re-ask rhythm is something a user can learn to anticipate, which defeats
the point of asking again.

The state machine takes a `kotlin.random.Random` as a constructor parameter defaulting to
`Random.Default`. Production passes nothing. The one test that needs to assert a full queue sequence
passes a fixed `Random`; every other placement test asserts the invariant — the gap is always within
range, never 0 or 1 — over many repetitions, which is a stronger claim than pinning one sample. This
is the same pattern every randomizing unit in the app uses.

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

**Seeding the draw from a session-wide seed** — rejected, and the seed that once existed on
`StudySessionConfig` was removed with it. A session seed makes queue evolution reproducible from a
configuration, which is attractive for reproducing a report, but it also makes the re-ask rhythm
deterministic for a given session, and it threads a selection-time concern through the state machine
for a benefit only tests consume. A `Random` parameter with a stdlib default gives the tests
everything they need without putting a seed on a route, a config or session state.

## Consequences

- The session's queue is mutable, not a fixed list with an advancing index, and its length grows as
  cards are re-inserted. Any "card N of M" progress display must decide what M means; the honest
  reading is distinct cards, not queue entries.
- The gap constants are tunable and belong with the rest of the session's tunables rather than
  scattered in the state machine.
- Placement tests assert invariants over many repetitions rather than one sampled order; a fixed
  `Random` is available for the single test that needs a full sequence.
- Neither a reported session's queue order nor its card selection is reproducible from a
  configuration. Reproducing a report exactly is not a capability the app has, and buying it back
  would mean persisting a seed per session for a debugging use case that has never arisen.
- A card can be re-inserted past the end of the queue when few cards remain, so late-session
  failures naturally cluster toward the end regardless of the gap. This is unavoidable and
  acceptable.
