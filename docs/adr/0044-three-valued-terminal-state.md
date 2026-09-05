# Terminal State is three-valued, and the best Rating achieved wins

## Decision

A Flashcard's Terminal State in a Rated Study Session has three values: **Mastered**, **Partial**
and **Failed**.

The resolution rule is **the best Rating the card achieved across all its Attempts**:

| Best Rating achieved | Terminal State |
|---|---|
| Correct (any Attempt) | Mastered |
| Partial | Partial |
| Failed only | Failed |

A card rated Failed, then Partial, then Failed ends **Partial**, because Partial is the best the
user ever managed on it.

### Partial is XP-positive and mastery-neutral

- Terminal Partial awards a small positive amount — an `XpConfig` value
  ([ADR-0047](0047-xp-values-behind-a-config-repository.md)) — far below Mastered.
- It does not enter Persistent Mastery.
- A **Mastery Defense** card ending Partial **keeps its mastery**, earning neither the defense bonus
  nor the de-mastery penalty. Only Terminal Failed de-masters.

### Partial can end a card immediately

A user setting controls whether a Partial Rating re-queues the card or ends it on the spot. Default
off, so Partial re-queues. Turned on, a Partial Rating resolves the card to Terminal Partial
immediately.

The setting's label must disclose that a Partial card does not count as mastered — a label implying
"this card won't repeat" alone reads as a free pass.

## Context

Two forces arrived at the same missing concept from opposite directions.

**The progress rings.** Category Details shows a completion ring per Subcategory, and the user can
toggle it to a second perspective: how much of a topic they have been *through*, not just how much
they *own*. That needs a recorded outcome for "engaged with, not mastered", which a two-valued
Mastered/Failed model cannot express.

**The "Partial doesn't re-queue" setting.** Under two Terminal States that setting had no honest
landing place. Resolving Partial to Mastered awards full mastery XP and writes a half-known card
into Persistent Mastery, where it later returns as a Mastery Defense card the user cannot defend.
Resolving it to Failed is punitive enough that users learn to avoid the honest middle button, which
corrupts every metric downstream of the Rating.

The old rule — Partial on the final Attempt resolves to Failed — was a symptom of the same gap. "You
got it half right three times, so you get Failed" was a rounding artefact of having nowhere else to
put the outcome.

## Alternatives considered

**Two Terminal States; the second ring counts only cards that ended Failed** — rejected. The metric
*shrinks* when the user improves: master a card you had been failing and the ring goes down. A
progress indicator that regresses on progress is worse than none.

**Two Terminal States; drop the "Partial doesn't re-queue" setting** — rejected. It leaves the
Rating vocabulary three-valued while the outcome vocabulary is two-valued, so a Partial Rating's
only lasting effect is re-queueing. The button then means "try me again" rather than "I half knew
this", and the self-assessment is discarded.

**Latest Rating wins rather than best** — rejected. A user who finally gets a card right on attempt
two, then is re-queued by an unrelated rule and fumbles it on attempt three, should not lose what
they demonstrated.

**A fourth state for "attempted but never rated"** — rejected. A voice silence timeout consumes no
Attempt and marks nothing ([ADR-0031](0031-voice-answering-shared-tts-engine-silence-timeout-grade-bands.md)),
so there is no such outcome needing a home.

## Consequences

- The session's per-card state retains the **best Rating so far**, one enum field per card. That is
  the only Rating history the state machine needs — a full per-Attempt log is not required to
  resolve a Terminal State.
- `cardProgress.state` ([ADR-0016](0016-card-progress-model.md)) is four-valued — the three Terminal
  States plus `Seen` — not a boolean.
- The Session Summary's XP breakdown gains a Partial line.
- De-mastery narrows to Terminal Failed only, rather than "anything that is not Mastered".
- The `attemptsPerCard` limit still bounds a card's Attempts; what changes is only how the
  accumulated Ratings resolve when the limit is reached.
