package com.rossomak.flashcards.core.domain.model

import kotlin.random.Random

/**
 * A Rated Study Session's queue and per-card grading. Entirely pure Kotlin — no Android, no
 * coroutines, no repositories — so the Rated ViewModel holds one instance, applies events to it,
 * and maps the result into screen state.
 *
 * The queue is a mutable list that **grows**, not a fixed list with an advancing index: the current
 * card is always the head, and [rate] removes it from the head and may put it back further down
 * ([ADR-0046](../../../../../../../docs/adr/0046-failed-and-partial-re-insertion-placement.md)). A
 * card whose Attempts are exhausted — or whose Rating already resolves it — leaves the queue for
 * good, resolved to a [TerminalState]
 * ([ADR-0044](../../../../../../../docs/adr/0044-three-valued-terminal-state.md)). [isComplete]
 * becomes `true` exactly when every distinct card has reached a Terminal State, which coincides
 * with the queue emptying.
 *
 * @param cards the session's cards, in the order the Preview screen resolved them. One
 * [RatedSessionCardRecord] is seeded per card, none previously mastered — spec 07's job.
 * @param attemptsLimit how many Attempts a card gets before Attempts-exhausted resolution applies.
 * A limit of 1 is a legitimate strict setting, not a broken state: every Rating is then immediately
 * terminal and nothing is ever re-inserted.
 * @param partialRatingCardRequeueingEnabled the Preview screen's confirmed choice. `true` (the
 * default) lets a Partial Rating re-insert as normal; `false` resolves it to Terminal Partial on the
 * spot, never re-inserting.
 * @param random injected so tests can assert a full queue sequence with a fixed seed. Production
 * passes nothing — there is no session seed anywhere in the app, and none is to be added.
 */
class RatedSessionState(
    cards: List<Flashcard>,
    private val attemptsLimit: Int,
    private val partialRatingCardRequeueingEnabled: Boolean = true,
    private val random: Random = Random.Default,
) {
    private val queue: MutableList<RatedSessionCardRecord> =
        cards.map { card -> RatedSessionCardRecord(card = card) }.toMutableList()
    private val terminalStates = mutableMapOf<String, TerminalState>()

    /** Fixed at construction — a re-insertion never changes how many distinct cards there are. */
    val distinctCardCount: Int = cards.size

    /** How many distinct cards have resolved [TerminalState.Mastered] so far. */
    val masteredCount: Int get() = terminalStates.values.count { it == TerminalState.Mastered }

    /** `true` exactly when every distinct card has reached a Terminal State. */
    val isComplete: Boolean get() = queue.isEmpty()

    /** The card at the head of the queue — the one due for its next Attempt. `null` once complete. */
    val currentCard: Flashcard? get() = queue.firstOrNull()?.card

    /** The queue's cards in current order, current card first. */
    val remainingCards: List<Flashcard> get() = queue.map { it.card }

    /**
     * The current (head) card's own Rating history, in order — ticket 03's `FlashcardsAttemptIndicator`
     * source. Empty once [isComplete], since there is no head left.
     */
    val currentCardRatings: List<FlashcardRating> get() = queue.firstOrNull()?.ratings ?: emptyList()

    /**
     * Applies [rating] to the current (head) card. Returns the [TerminalState] the card resolved
     * to, or `null` when it was re-inserted rather than finished.
     */
    fun rate(rating: FlashcardRating): TerminalState? {
        val record = queue.removeAt(0)
        val ratedRecord = record.copy(ratings = record.ratings + rating)

        val terminal = resolveTerminalState(ratedRecord, rating)
        if (terminal == null) {
            reinsert(ratedRecord, rating)
            return null
        }
        terminalStates[ratedRecord.card.id] = terminal
        return terminal
    }

    /** Resolution order per ADR-0044: Correct, then an immediately-terminal Partial, then Attempts-exhausted. */
    private fun resolveTerminalState(record: RatedSessionCardRecord, rating: FlashcardRating): TerminalState? = when {
        rating == FlashcardRating.Correct -> TerminalState.Mastered
        rating == FlashcardRating.PartiallyCorrect && !partialRatingCardRequeueingEnabled -> TerminalState.Partial
        record.attemptsUsed >= attemptsLimit ->
            if (record.bestRating == FlashcardRating.PartiallyCorrect) TerminalState.Partial else TerminalState.Failed
        else -> null
    }

    /** Draws a bounded random gap (ADR-0046) and inserts, clamped to the end of the remaining queue. */
    private fun reinsert(record: RatedSessionCardRecord, rating: FlashcardRating) {
        val (minGap, maxGap) = when (rating) {
            FlashcardRating.Failed ->
                StudySessionConfig.FAILED_REQUEUE_MIN_GAP to StudySessionConfig.FAILED_REQUEUE_MAX_GAP
            FlashcardRating.PartiallyCorrect ->
                StudySessionConfig.PARTIAL_REQUEUE_MIN_GAP to StudySessionConfig.PARTIAL_REQUEUE_MAX_GAP
            FlashcardRating.Correct ->
                error("Correct never re-inserts — it resolves Mastered immediately in resolveTerminalState")
        }
        val gap = random.nextInt(minGap, maxGap + 1)
        queue.add(gap.coerceAtMost(queue.size), record)
    }
}
