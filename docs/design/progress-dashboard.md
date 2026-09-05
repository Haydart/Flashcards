# Progress Dashboard Screen

## Navigation

Fourth bottom tab added to the main shell:

```
Home · Study · Progress · Settings
```

The Progress tab sits between Study and Settings — mirrors the natural user loop: discover → study → review progress → configure.

Tab is backed by a `ProgressGraph` nested graph inside `Main`, following the same pattern as `HomeGraph` and `StudyGraph`.

## Screen layout (top to bottom)

### 1. Level card
- Displays current level number prominently
- XP progress bar: current XP within the level / XP needed for next level
- Tapping the card has no action (informational only)
- Level-up animation triggers here when a new level is reached (see [XP & Leveling System](xp-leveling-system.md))

### 2. Daily goal section
- Circular progress ring: today's studied minutes / daily goal minutes
- Goal value is tappable → opens a number picker to edit the goal inline
- No navigation away from the screen required
- Label: "X of Y min today"

### 3. Streak row
- Current streak (days) displayed with a flame icon
- Best streak (historical peak) shown as a secondary value on the same row

### 4. Weekly study time
- Total minutes studied in the current calendar week (Mon–Sun, device local time)
- Single stat line, no breakdown

### 5. Stats row (summary figures)
Four figures displayed in a horizontal row:
- Total study time (all-time, in hours/minutes)
- Sessions completed (all-time, deck-end sessions only — see [Session Stats & Data Model](session-stats-data-model.md))
- Cards Studied (both Study Modes; monotonic — a card never leaves the Studied set)
- Cards Mastered (currently held, Rated sessions only — **decreases on de-mastery**, mirroring the live Persistent Mastery set rather than a lifetime "ever mastered" tally)

### 6. History chart
- Stacked bar chart; each bar = one calendar day; bar height = total minutes studied
- Each colored segment within a bar = one Category (colors consistent with Category identity across the screen)
- Toggle above chart: **7 days** (default) / **30 days**
- No interaction on bars in MVP

### 7. Per-category breakdown
- Inline section immediately below the chart; serves as the chart legend
- One row per Category studied this week: Category name + color swatch + minutes this week
- Rows ordered by time studied (descending)
- Tapping a Category row: no action in MVP (future: Subcategory breakdown drill-down)

## Daily goal setup

- Set during onboarding (skippable flow)
- Default if skipped: **20 minutes/day**
- Editable at any time via the tap-to-edit interaction on the Progress screen (section 2 above)
- Stored in DataStore locally and in Firestore on the user document for cross-device sync

## Data sources

| Stat | Source |
|---|---|
| Level, XP, streak, best streak, daily goal | DataStore cache (written on session end) |
| Today's minutes, weekly minutes | DataStore cache (written on session end) |
| Total time, sessions completed, cards Studied, cards Mastered | DataStore cache (written on session end) |
| History chart data (7/30 days) | Recomputed from Firestore session records |
| Per-category breakdown | Recomputed from Firestore session records |

On fresh install / reinstall: XP, level and streak are read straight off `users/{uid}/state/progression`; the daily goal comes from local preferences, which are device-scoped and reset on reinstall; time-windowed aggregates are recomputed from Firestore session history; the two card counts come from the `state/progressSummary` document. DataStore is repopulated before the Progress screen is shown.
