# User Flow — Progress

NYI — flows designed from ADRs and design docs. See [docs/design/progress-dashboard.md](../design/progress-dashboard.md), [docs/design/launcher-widget.md](../design/launcher-widget.md), [docs/design/session-stats-data-model.md](../design/session-stats-data-model.md).

## Entry points & screen

Widget tap on the device home screen opens the app and navigates directly to the Progress tab. If the app cold-starts, auth check runs first (see [main.md](main.md)).

```mermaid
flowchart TD

    %% Legend: (Screen)  [/Action/]  {Decision}  ([Entry/Exit])  {{NYI}}

    %% ── Entry points ──────────────────────────────────────────────
    WidgetTap([Launcher widget tap\n2×2 or 4×2]) --> ProgressScreen
    ProgressTab([Progress tab]) --> ProgressScreen

    %% ── Progress Screen ───────────────────────────────────────────
    ProgressScreen(PROGRESS SCREEN\nNYI)

    %% Section 1 — Level card
    ProgressScreen --> LevelCard[Level card\nLevel number · XP progress bar\nno action]

    %% Section 2 — Daily goal
    ProgressScreen --> TapGoalRing[/Tap daily goal ring\ncircular progress: today / goal min/]
    TapGoalRing --> GoalPicker(GOAL PICKER\nnumber input dialog)
    GoalPicker -->|set new goal| ProgressScreen

    %% Section 3 — Streak
    ProgressScreen --> StreakRow[Streak row\ncurrent streak · best streak\nno action]

    %% Section 4 — Weekly time
    ProgressScreen --> WeeklyTime[Weekly study time\nno action]

    %% Section 5 — Stats row
    ProgressScreen --> StatsRow[Stats row\ntotal time · sessions completed · cards mastered\nno action]

    %% Section 6 — History chart
    ProgressScreen --> HistoryChart[History chart\nstacked bar by category · one bar per day]
    HistoryChart --> ChartToggle[/Toggle: 7 days ↔ 30 days/]
    ChartToggle --> HistoryChart

    %% Section 7 — Per-category breakdown
    ProgressScreen --> PerCategory[Per-category breakdown\ncategory · color swatch · minutes this week\nordered by time studied desc\nno action in MVP]
```

## Widget sizes

| Size | Content |
|---|---|
| 2×2 small | Streak (flame + day count) · Daily goal ring (X / Y min) |
| 4×2 medium | Level number + XP progress bar · Streak · Today's goal · Weekly minutes |

Widget reads from DataStore only — no Firestore reads at widget update time. Updated after each session end. Shows placeholder state before first session.

## Data sources

| Stat | Source | When updated |
|---|---|---|
| Level, XP, streak, best streak | DataStore cache | Each session end |
| Today's minutes, weekly minutes | DataStore cache | Each session end |
| Total time, sessions completed, cards mastered | DataStore cache | Each session end |
| History chart (7/30 days) | Recomputed from Firestore | Progress screen load |
| Per-category breakdown | Recomputed from Firestore | Progress screen load |
