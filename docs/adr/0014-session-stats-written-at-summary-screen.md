# Session stats written once at Session Summary screen

All Study Session statistics (duration, cards mastered, XP, mastered card updates) are written to Firestore in a single batch when the Session Summary screen is shown. No writes occur during a session — not per-card, not per-rating.

We considered incremental writes: writing each Mastered Terminal State to Firestore as it occurs. Rejected because: (1) a 150-card Rated session could generate up to 150 individual Firestore writes, which does not scale to many concurrent users; (2) partial-session semantics become ambiguous — if the user exits mid-session, half-written progress is in Firestore with no clean boundary; (3) the session ViewModel already holds all state in memory, so nothing is lost by deferring to the end.

The Session Summary screen is the mandatory exit path for all sessions — including partial sessions (back-press/X → confirmation dialog → Summary). This guarantees the write always fires exactly once per session with complete data.

Consequence: if the app crashes mid-session, no stats for that session are recorded. Acceptable for an academic project; a future mitigation could persist in-progress state to DataStore and recover on next launch.
