# Session Stats & Data Model

## Write point

All session statistics are written to Firestore **once, at the Session Summary screen**. No incremental writes occur during a session (not per-card, not per-rating). This keeps the write path simple and scales to many users without hot-document contention.

## Session completion vs partial sessions

**Full session:** User reaches deck end (last card completed). Session Summary is shown automatically.

**Partial session:** User presses back or taps the X button mid-session. A confirmation dialog is shown. On confirmation, the user is redirected to the Session Summary screen — which triggers the Firestore write with the stats accumulated up to exit.

Partial sessions:
- Count toward streak (a session was started and reached summary)
- Count toward daily goal (time studied is recorded)
- Count toward time-based stats (total time, history chart, per-category breakdown)
- Do **not** count toward "Sessions Completed" (deck end was not reached)
- Are flagged with `isPartial: true` in the session document

There is no way to exit a Study Session without passing through the Session Summary screen (and triggering the write).

## Session duration

Duration = time from session start to deck end (last card completed) or to exit confirmation (partial). Time spent on the Session Summary screen is excluded.

## Streak rules

- **Definition of a study day:** any session that reaches the Session Summary screen (partial or full)
- **Day boundary:** midnight in the device's local timezone at session start time
- **Streak increments** when the user completes at least one session on a calendar day with no preceding session that day already counted
- **Streak breaks** when a full calendar day passes with no session
- **Best streak** is the historical maximum; never decremented

## Daily goal

- Unit: minutes studied per day
- Default: 20 minutes
- Set during onboarding (skippable); editable inline on the Progress screen
- Goal met = today's total studied minutes ≥ daily goal minutes
- XP awarded once per calendar day when the goal is first met (not on subsequent sessions the same day)

## Firestore schema

### Session record: `users/{uid}/sessions/{sessionId}`

| Field | Type | Notes |
|---|---|---|
| `sessionId` | String | Auto-generated |
| `startTimestamp` | Timestamp | UTC; streak/day computation uses device local time at query |
| `durationSeconds` | Int | Start → deck end or exit confirmation |
| `studyMode` | Enum | `FAST` or `RATED` |
| `categoryId` | String | |
| `subcategoryIds` | List\<String\> | One or more |
| `cardsMastered` | Int | Rated sessions only; 0 for Fast |
| `isPartial` | Boolean | `true` if user exited before deck end |

### User document: `users/{uid}`

Stats fields added:
| Field | Type | Notes |
|---|---|---|
| `xp` | Long | Total cumulative XP earned |
| `level` | Int | Current level (denormalized) |
| `xpIntoCurrentLevel` | Long | XP within current level (for progress bar) |
| `currentStreak` | Int | Current streak in days |
| `bestStreak` | Int | Historical peak streak |
| `dailyGoalMinutes` | Int | User's daily time goal |

### Mastered cards: `users/{uid}/masteredCards/{cardId}`

See [Persistent Card Mastery](persistent-card-mastery.md).

## DataStore cache (widget and offline)

Aggregates are pre-computed on each session end and written to DataStore for fast local reads (widget, offline Progress screen).

| Key | Value | Updated |
|---|---|---|
| `currentStreak` | Int | Each session end |
| `bestStreak` | Int | Each session end |
| `todayMinutes` | Int | Each session end |
| `weeklyMinutes` | Int | Each session end |
| `dailyGoalMinutes` | Int | Each goal change or session end |
| `level` | Int | Each session end |
| `xpIntoCurrentLevel` | Long | Each session end |
| `xpForNextLevel` | Long | Each session end |
| `totalMinutes` | Long | Each session end |
| `totalSessionsCompleted` | Int | Each full-session end |
| `totalCardsMastered` | Int | Each session end |

History chart data (7/30 days) and per-category breakdown are **not** cached in DataStore — recomputed from Firestore session records at runtime when the Progress screen loads.

## Cross-device sync / reinstall

On fresh install or reinstall:
1. Fetch all `users/{uid}/sessions` documents from Firestore
2. Recompute all DataStore aggregates from session history
3. Write aggregates to DataStore
4. Show Progress screen

This ensures stats are never permanently lost, only temporarily unavailable until Firestore sync completes.
