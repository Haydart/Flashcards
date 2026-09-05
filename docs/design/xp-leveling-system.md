# XP & Leveling System

## Purpose

RPG-style progression layer to improve user retention. XP is earned by studying and mastering Flashcards. Level reflects cumulative effort. The system naturally incentivises Rated sessions over Fast sessions without making Fast sessions feel pointless.

## XP sources

| Source | Amount | Notes |
|---|---|---|
| New Flashcard studied | **10 XP per card** (flat) | Both modes. Awarded once ever per Flashcard, the first time it enters **Studied**. Never for Private Flashcards. |
| Card Mastered (Rated) | **100 XP per card** (flat) | Rated only. Flat rate regardless of difficulty. |
| Card Partial (Rated) | **25 XP per card** (flat) | Rated only. Terminal State Partial — never Correct, but Partial at least once. |
| Mastery Defended (Correct on any Attempt) | **50 XP per card** (flat) | Rated only. See [Card Progress and Persistent Mastery](persistent-card-mastery.md). |
| Card de-mastered (Failed Terminal State on a previously mastered card) | **−80 XP per card** (flat) | Rated only. A **Partial** Terminal State on a defended card is neutral — no bonus, no penalty. |
| Session fully completed (deck end reached) | **+500 XP** | Both modes. Partial sessions do not earn this. |
| Daily goal met | **+1000 XP** | Awarded once per calendar day, when today's studied minutes first reach the goal. |
| Streak continuation | `min(streakDays × 250, 2500)` XP | Day 1 = 250, Day 2 = 500, …, Day 10+ = 2500 (cap). Once per calendar day on which a session reaches the Summary screen. |
| Time studied | **10 XP per minute** | Both modes. Session duration, first card shown → deck end or exit. |

> **These numbers are configuration, not domain rules.** Every value above, and the level-curve parameters below, live in `XpConfig` behind `XpConfigRepository` — see [ADR-0047](../adr/0047-xp-values-behind-a-config-repository.md). The implementation is local and hardcoded today; the seam exists so the balance can be tuned without shipping a release. A session is scored against the config captured when it **started**, so a config change can never retroactively rewrite a session already in progress.

Flat per-card rates keep the session summary equation readable: `{N} cards × {rate} = {XP}`.

## Fast vs Rated XP

Fast sessions earn: new-card XP, time XP, session completion XP, streak XP, daily goal XP.

Fast sessions do not earn: card mastery XP, Partial XP, mastery defense XP, and cannot suffer de-mastery XP loss.

A Fast-only user therefore still sees their coverage and their XP grow — just more slowly than a Rated user, and without ever touching Persistent Mastery.

## Level curve

Shape: super-linear (fast early levels, increasingly expensive later ones). Constraints:

- XP required per level increases with level number (not constant)
- Early levels reachable within 1–2 good sessions
- Mid-range levels (10–25) require consistent multi-day effort
- High levels (50+) are long-term achievements
- All per-level XP thresholds rounded up to the nearest 1000 XP

Formula: `ceil(base × level^exponent / 1000) × 1000`, where `base` and `exponent` are `XpConfig` values rather than constants — so hitting the milestone targets above is a tuning exercise, not a code change.

## Level-up rewards

- Confetti/celebration animation on the Progress screen, and on the Session Summary screen when the level-up happened during that session
- Milestone badges unlocked at levels 5, 10, 25, 50, 100 — displayed on the Progress screen
- Badge/achievement system detail: deferred to a separate design session
- No XP burst on level-up (keeps the curve clean)

## Session Summary XP presentation

XP is calculated **on the Session Summary screen**, from the session result and the `XpConfig` snapshot the session carried, and written to Firestore there as part of the single session-commit batch ([ADR-0014](../adr/0014-session-stats-written-at-summary-screen.md)). The screen shows an itemized breakdown as a sequential "pour" animation in the dark header zone (after the mastery ring sweep for Rated sessions).

### Animation sequence

1. **Total XP counter** appears prominently at top, starting at 0.
2. First **item tile** slides up from below into view, showing a math equation:
   - `{N} new cards × 10 = {XP}` (New Cards)
   - `{N} cards × 100 = {XP}` (Card Mastery)
   - `{N} cards × 25 = {XP}` (Partial)
   - `{N} cards × 50 = {XP}` (Mastery Defense)
   - `{N} cards × 80 = -{XP}` (De-mastery)
   - `{N} min × 10 = {XP}` (Time Studied)
   - `{N} day streak × 250 = {XP}` (Streak, capped display)
   - `Session completed = +500` (flat, no multiplier)
   - `Daily goal met = +1000` (flat, no multiplier)
3. The XP value after the `=` sign counts **down to 0** while the total counter counts **up** by the same amount simultaneously ("pouring" the number into the total).
4. Item disappears once its value reaches 0. Next item slides up.
5. After all items are consumed: session total animates into the user's overall XP progress.
6. If a level-up occurred: level-up celebration (confetti + new level). Multiple level-ups play sequentially.

### Rules

- Items worth 0 XP are omitted entirely
- De-mastery items use error color (red) for the equation and the pour
- `Session Completed` omitted for partial sessions
- For Rated sessions, the mastery ring sweep plays **before** the XP pour begins
- For Fast sessions, the pour begins immediately (no ring phase)

## Firestore storage

Stored on the user document, `users/{uid}`:

- `xp`: total XP currently held
- `level`: current level (denormalized for fast reads)
- `xpIntoCurrentLevel`: XP accumulated within the current level
- `lastStudyDate`: local calendar date of the most recent session counted toward the streak
- `goalMetDate`: local calendar date on which the daily goal was most recently met

`lastStudyDate` and `goalMetDate` are load-bearing, not conveniences: streak continuation needs to know whether a session today has already been counted, and the daily-goal award needs to know whether today's goal was already met. Neither is derivable from the other stored fields without replaying session history.

De-mastery reduces both `xp` and `xpIntoCurrentLevel`. **Level never decreases** — if a loss would push `xpIntoCurrentLevel` below 0, it clamps to 0 at the current level's floor, and `xp` clamps by the same amount so the two stay consistent.

Day attribution uses a session's **start** timestamp in the device's local timezone, so a session crossing midnight counts toward the day it began.
