# DataStore for pre-computed stats cache (not Room)

Stats aggregates (streak, daily goal progress, weekly time, level, XP) are cached locally in DataStore as pre-computed key-value pairs. Room (SQLite) was considered and rejected.

Room would be appropriate if we needed to query raw session records locally (e.g. "sum of duration WHERE date >= last 7 days" without a Firestore read). We do not: Firestore is the source of truth for raw session history; the widget and offline Progress screen only need the final aggregate values, which are flat scalars.

DataStore holds ~10 integer/long values. These are recomputed from Firestore session records on each session end and written atomically to DataStore. On reinstall, Firestore session history is re-fetched and aggregates are recomputed before the Progress screen is shown.

History chart data (7/30 days) and per-category breakdown are not cached in DataStore — they require per-day, per-category resolution that does not reduce to a flat key-value set. These are recomputed from Firestore at runtime when the Progress screen loads.

Consequence: if offline, the Progress screen shows stale chart/breakdown data from the last Firestore sync. Widget stats remain accurate from DataStore.
