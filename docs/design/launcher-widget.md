# Launcher Widget

## Purpose

Home screen widget providing at-a-glance study progress without opening the app. Purely informational — no quick-start button. Tap anywhere opens the Progress screen.

## Sizes

### Small (2×2 cells)
- Current streak value + flame icon
- Circular progress ring: today's studied minutes / daily goal minutes
- Label: "X of Y min"

### Medium (4×2 cells)
- Level number + XP progress bar (current XP / XP to next level)
- Current streak (flame icon + day count)
- Today's goal progress: "X / Y min"
- Weekly minutes studied

Layout sketch:
```
Lvl 12  ████████░░  4200 / 5000 XP
🔥 8-day streak          18 / 25 min today
                         47 min this week
```

## Data source

Widget reads exclusively from **DataStore** — no Firestore reads at widget update time. DataStore is updated by the app on each session end (see [Session Stats & Data Model](session-stats-data-model.md)).

Fields read from DataStore: `level`, `xpIntoCurrentLevel`, `xpForNextLevel`, `currentStreak`, `todayMinutes`, `dailyGoalMinutes`, `weeklyMinutes`.

If DataStore has no data yet (first install, not yet logged in), widget shows a placeholder state ("Start studying to see your progress").

## Widget update trigger

`AppWidgetManager.updateAppWidget` is called from the app process after each session-end DataStore write. No background polling or `AlarmManager` scheduling in MVP — widget updates only when the app runs a session.

## Tap behavior

Any tap on either widget size → opens the app and navigates directly to the Progress tab.

## Not included

- Quick-start / "Start session" button
- History chart
- Per-category breakdown
- Sessions completed or total cards mastered

These stats are available on the Progress screen; the widget surface is insufficient for them.
