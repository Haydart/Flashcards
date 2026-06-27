# XP & Leveling System

## Purpose

RPG-style progression layer to improve user retention. XP is earned by studying and mastering Flashcards. Level reflects cumulative effort. The system naturally incentivises Rated sessions over Fast sessions without blocking either.

## XP sources

| Source | Amount | Notes |
|---|---|---|
| Card Mastered (Rated) | `difficulty × 10` | Range 10–100 XP per card. Rated sessions only. |
| Mastery Defended (Correct on any Attempt) | `difficulty × 5` | Range 5–50 XP. Rated sessions only. See [Persistent Card Mastery](persistent-card-mastery.md). |
| Card de-mastered (Failed Terminal State on previously mastered card) | `-(difficulty × 8)` | Range −8 to −80 XP. Rated sessions only. |
| Session fully completed (deck end reached) | **+500 XP** | Both Fast and Rated. Partial sessions do not earn this. |
| Daily goal met | **+1000 XP** | Awarded once per calendar day when today's studied minutes ≥ daily goal. |
| Streak continuation | `min(streakDays × 250, 2500)` XP | Day 1 = 250, Day 2 = 500, …, Day 10+ = 2500 (cap). Awarded once per calendar day a session is started. |
| Time studied | **10 XP per minute** | Both Fast and Rated. Based on session duration (start → deck end or exit). |

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

XP is calculated and written to Firestore at the end of every session (on the Session Summary screen). The summary screen shows an itemized XP breakdown with sequential animation:

```
Card Mastery          +640 XP
Mastery Defense       +120 XP
Session Completed     +500 XP
Daily Goal Met       +1000 XP
Streak Day 8         +2000 XP
Time Studied (18 min) +180 XP
──────────────────────────────
Total               +4440 XP   → Level 12!
```

- Each line animates in sequentially (top to bottom)
- Lines with 0 XP are omitted
- XP loss lines (de-mastery) are shown in a distinct color (red/error)
- If a level-up occurs, it is revealed after the total line with the celebration animation
- If the session is partial, "Session Completed" line is omitted

## Firestore storage

XP and level are stored on the user document: `users/{uid}`

Fields:
- `xp`: total cumulative XP earned (never decremented; level is derived)
- `level`: current level (denormalized for fast reads)
- `xpIntoCurrentLevel`: XP accumulated within the current level

XP loss (de-mastery) reduces `xpIntoCurrentLevel` and `xp`. Level cannot decrease — if de-mastery pushes `xpIntoCurrentLevel` below 0, it is clamped to 0 at the current level boundary.
