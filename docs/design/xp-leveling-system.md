# XP & Leveling System

## Purpose

RPG-style progression layer to improve user retention. XP is earned by studying and mastering Flashcards. Level reflects cumulative effort. The system naturally incentivises Rated sessions over Fast sessions without blocking either.

## XP sources

| Source | Amount | Notes |
|---|---|---|
| Card Mastered (Rated) | **100 XP per card** (flat) | Rated sessions only. Flat rate regardless of difficulty. |
| Mastery Defended (Correct on any Attempt) | **50 XP per card** (flat) | Rated sessions only. See [Persistent Card Mastery](persistent-card-mastery.md). |
| Card de-mastered (Failed Terminal State on previously mastered card) | **−80 XP per card** (flat) | Rated sessions only. |
| Session fully completed (deck end reached) | **+500 XP** | Both Fast and Rated. Partial sessions do not earn this. |
| Daily goal met | **+1000 XP** | Awarded once per calendar day when today's studied minutes ≥ daily goal. |
| Streak continuation | `min(streakDays × 250, 2500)` XP | Day 1 = 250, Day 2 = 500, …, Day 10+ = 2500 (cap). Awarded once per calendar day a session is started. |
| Time studied | **10 XP per minute** | Both Fast and Rated. Based on session duration (start → deck end or exit). |

> **Note:** Flat per-card rates (100 / 50 / −80) are tunable constants, chosen at implementation time. Flat rates make the session summary equation readable: `{N} cards × {rate} = {XP}`.`

## Fast vs Rated XP

Fast sessions earn: time XP + session completion XP.
Fast sessions do not earn: card mastery XP, mastery defense XP, de-mastery XP loss.

This difference naturally drives users toward Rated sessions for higher XP yield without making Fast sessions feel pointless.

## Level curve

Shape: super-linear (fast early levels, increasingly expensive later levels). Exact formula determined at implementation time. Constraints:

- XP required per level increases with level number (not constant)
- Early levels reachable within 1–2 good sessions
- Mid-range levels (10–25) require consistent multi-day effort
- High levels (50+) are long-term achievements
- All per-level XP thresholds rounded up to the nearest 1000 XP

Suggested formula shape: `ceil(base × level^exponent / 1000) × 1000` — tune `base` and `exponent` during implementation to hit the milestone targets above.

## Level-up rewards

- Confetti/celebration animation triggered on the Progress screen (and surfaced on the Session Summary screen if a level-up occurs during the session)
- Milestone badges unlocked at levels: 5, 10, 25, 50, 100 — displayed on the Progress screen
- Badge/achievement system detail: deferred to a separate design session
- No XP burst on level-up (keeps the curve clean)

## Session Summary XP presentation

XP is calculated and written to Firestore at the end of every session (on the Session Summary screen). The summary screen shows an itemized XP breakdown as a sequential "pour" animation in the dark header zone (after the mastery ring sweep for Rated sessions).

### Animation sequence

1. **Total XP counter** appears prominently at top, starting at 0.
2. First **item tile** slides up from below into view, showing a math equation:
   - `{N} cards × 100 = {XP}` (Card Mastery)
   - `{N} cards × 50 = {XP}` (Mastery Defense)
   - `{N} cards × 80 = -{XP}` (De-mastery)
   - `{N} min × 10 = {XP}` (Time Studied)
   - `{N} day streak × 250 = {XP}` (Streak, capped display)
   - `Session completed = +500` (flat, no multiplier)
   - `Daily goal met = +1000` (flat, no multiplier)
3. The XP value after the `=` sign counts **down to 0** while the total counter counts **up** by the same amount simultaneously ("pouring" the number into the total).
4. Item disappears once its value reaches 0. Next item slides up.
5. After all items consumed: total XP from session animates into the user's overall XP progress.
6. If a level-up occurred: level-up celebration appears (confetti + new level display). Multiple level-ups in one session play sequentially.

### Rules

- Items with 0 XP are omitted entirely
- De-mastery items use error color (red) for the equation and the pour animation
- `Session Completed` line omitted for partial sessions
- For Rated sessions, mastery ring sweep plays **before** the XP pour sequence begins
- For Fast sessions, XP pour begins immediately (no ring phase)

## Firestore storage

XP and level are stored on the user document: `users/{uid}`

Fields:
- `xp`: total cumulative XP earned (never decremented; level is derived)
- `level`: current level (denormalized for fast reads)
- `xpIntoCurrentLevel`: XP accumulated within the current level

XP loss (de-mastery) reduces `xpIntoCurrentLevel` and `xp`. Level cannot decrease — if de-mastery pushes `xpIntoCurrentLevel` below 0, it is clamped to 0 at the current level boundary.
