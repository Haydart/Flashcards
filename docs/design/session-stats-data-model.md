# Session Stats & Data Model

## Write point

All session statistics are written to Firestore **once, at the Session Summary screen**, as a single atomic batch. No incremental writes occur during a session — not per card, not per rating, not per voice grade. This keeps the write path simple, scales without hot-document contention, and means a session is either fully recorded or not recorded at all. See [ADR-0014](../adr/0014-session-stats-written-at-summary-screen.md).

The batch contains, in one commit:

- the session document, with its per-card ledger embedded
- one packed `progress/{subcategoryId}` document per Subcategory the session touched
- `users/{uid}/state/progressSummary` counter increments
- `users/{uid}/state/progression` — the scoring state
- the user document's XP, level and streak fields

A single-Subcategory session is therefore **four writes**, whatever its length.

## Session completion vs partial sessions

**Full session:** User reaches deck end. Session Summary is shown automatically.

**Partial session:** User presses back or taps the X button mid-session. A confirmation dialog is shown. On confirmation the user is taken to the Session Summary screen, which triggers the write with everything accumulated up to that point. Cards still in the queue at exit are simply never recorded.

Partial sessions:
- Count toward streak (the session reached the Summary screen)
- Count toward daily goal (time studied is recorded)
- Count toward time-based stats (total time, history chart, per-category breakdown)
- Record card progress for every card that actually reached the user
- Do **not** count toward "Sessions Completed" (deck end was not reached)
- Do **not** earn session-completion XP
- Are flagged `isPartial: true`

There is no way to exit a Study Session without passing through the Session Summary screen. An app kill or crash is the exception, and records nothing at all — not a partial session, but no session.

## Session duration

Duration = first card shown → deck end or exit confirmation. The clock starts when a card is actually on screen, not when the route is entered, so a session whose card load fails banks nothing.

Backgrounded time accrues **only while voice playback is active**. A backgrounded Fast read-aloud session is genuinely studying and keeps counting; a backgrounded Rated session does not. Time on the Session Summary screen is excluded.

## Streak rules

- **Definition of a study day:** any session that reaches the Session Summary screen (partial or full)
- **Day boundary:** midnight in the device's local timezone, applied to the session's **start** timestamp — so a session crossing midnight counts toward the day it began
- **Streak increments** when a session reaches Summary on a calendar day later than `lastStudyDate`
- **Streak breaks** when a full calendar day passes with no session
- **Best streak** is the historical maximum; never decremented

## Daily goal

- Unit: minutes studied per day
- Default: 20 minutes
- Set during onboarding (skippable); editable inline on the Progress screen
- Goal met = today's total studied minutes ≥ daily goal minutes
- XP awarded once per calendar day, gated on `goalMetDate`, when the goal is first met

## Firestore schema

### Session record: `users/{uid}/sessions/{sessionId}`

**One session is one document.** Aggregates, denormalized names, and the per-card ledger embedded as a map. Home's Recents carousel is this collection's highest-traffic reader, and reads it with `orderBy(startTimestamp).limit(n)`, whose cost is the limit rather than the collection size.

| Field | Type | Notes |
|---|---|---|
| `sessionId` | String | Auto-generated |
| `startTimestamp` | Timestamp | UTC; day attribution uses device local time |
| `durationSeconds` | Int | See [Session duration](#session-duration) |
| `studyMode` | Enum | `FAST` or `RATED` |
| `isPartial` | Boolean | `true` if the user exited before deck end |
| `categoryId` | String | |
| `categoryName` | String | Denormalized — Recents renders it without a join |
| `subcategoryIds` | List\<String\> | One or more |
| `subcategoryNames` | List\<String\> | Denormalized, same reason |
| `cardCount` | Int | Distinct Flashcards the session put in front of the user |
| `cardsMastered` | Int | Rated only; 0 for Fast |
| `cardsPartial` | Int | Rated only; 0 for Fast |
| `cardsDefended` | Int | Rated only; mastery held under Mastery Defense |
| `cardsDemastered` | Int | Rated only; mastery lost |
| `newCardsStudied` | Int | Both modes; Flashcards entering **Studied** for the first time |

The four per-outcome counts exist so the Summary's XP breakdown is reproducible from the stored record, rather than only from the transient in-memory result.

#### The embedded ledger: `outcomes`

A map on the session document, keyed by card id, holding one entry per card the session actually recorded.

| Field | Type | Notes |
|---|---|---|
| `subcategoryId` | String | Which topic the card belongs to |
| `state` | Enum | `Mastered` \| `Partial` \| `Failed` \| `Seen` (`Seen` for Fast) |
| `attemptsUsed` | Int | Rated only; 0 for Fast |
| `wasPreviouslyMastered` | Boolean | Whether this was a Mastery Defense card |
| `transcript` | String? | Voice-answered cards only |

It is embedded rather than held in a subcollection because Firestore bills per document read: Recents pays the same read count either way, so splitting bought no read saving while costing a write per card. Size is bounded — one entry per distinct card, capped by the maximum session length — at roughly 13 KB in the worst case. See [ADR-0014](../adr/0014-session-stats-written-at-summary-screen.md).

Recents transfers this map without rendering it, since the Android client SDK has no field projection. Revisit only if measured.

### Scoring state: `users/{uid}/state/progression`

| Field | Type | Notes |
|---|---|---|
| `xp` | Long | Total XP currently held |
| `level` | Int | Current level (denormalized); never decreases |
| `xpIntoCurrentLevel` | Long | XP within current level (for the progress bar); never negative |
| `currentStreak` | Int | Current streak in days |
| `bestStreak` | Int | Historical peak streak; never decreases |
| `lastStudyDate` | String | `yyyy-MM-dd` local calendar date of the last streak-counted session |
| `goalMetDate` | String | `yyyy-MM-dd` local calendar date the daily goal was last met |

The two dates are strings rather than Timestamps: they are calendar days in the device's local zone, not instants, and the only questions asked of them are same-day and later-day. String comparison on an ISO date answers both without a timezone-shift hazard.

**`dailyGoalMinutes` is not here.** It lives in local preferences alongside the other device-scoped settings, where Settings already writes it. A Firestore copy would be a second writable source with no sync story. The cost is that the goal does not follow a user to a new device — the same trade-off `hasSeenOnboarding` already makes explicitly.

**Scoring state is not on `users/{uid}` itself.** That document carries `entitlement`, which only the Admin SDK writes and the premium Cloud Function reads server-side, and it has no client rule in either direction. Making it client-writable so the Summary could save XP would let a user grant themselves premium. See [ADR-0014](../adr/0014-session-stats-written-at-summary-screen.md).

### Card progress: `users/{uid}/progress/{subcategoryId}` and `users/{uid}/state/progressSummary`

See [Card Progress and Persistent Mastery](persistent-card-mastery.md).

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
| `totalCardsStudied` | Int | Each session end |

`totalCardsMastered` mirrors the live Persistent Mastery set and therefore **decreases on de-mastery**, matching the progress summary's mastered counts. `totalCardsStudied` is monotonic. Neither is a lifetime "ever mastered" tally.

History chart data (7/30 days) and per-category breakdown are **not** cached — they need per-day, per-category resolution that does not reduce to a flat key-value set, and are recomputed from Firestore when the Progress screen loads.

## Cross-device sync / reinstall

On fresh install or reinstall:

1. Read `users/{uid}/state/progression` for `xp`, `level`, `xpIntoCurrentLevel`, `currentStreak`, `bestStreak`, `lastStudyDate`, `goalMetDate` — these are authoritative and are **not** replayed from history. `dailyGoalMinutes` is device-scoped local state and resets to its default on reinstall
2. Fetch `users/{uid}/sessions` to recompute the time-windowed aggregates that genuinely need history (`todayMinutes`, `weeklyMinutes`, `totalMinutes`, `totalSessionsCompleted`)
3. Read `state/progressSummary` — one document — to recompute `totalCardsMastered` and `totalCardsStudied`
4. Write aggregates to DataStore
5. Show Progress screen

XP, level and streak are stored as computed scalars precisely so they never have to be re-derived by replaying the XP formula over session history — which would also give the wrong answer if the XP config had changed in the interim.
