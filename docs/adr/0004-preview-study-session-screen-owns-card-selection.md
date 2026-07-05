# Preview Study Session Screen owns card selection

The Preview Study Session Screen receives `categoryId` and `subcategoryIds` and is solely responsible for selecting and randomizing the Flashcard pool before the session begins. `StudySession` receives the already-resolved `cardIds` list and does not perform selection itself.

We considered having each entry point (Quick Session button, fast-start row, Subcategory Details Start Session) resolve the card list before navigating. Rejected because it would scatter selection logic across multiple ViewModels (CategoryDetails, SubcategoryDetails) and prevent the Preview Study Session Screen from offering re-randomization or future parameter controls (card count sliders, difficulty weighting) — those controls only make sense on the screen that owns selection. `StudySession` stays a pure playback screen with no knowledge of how cards were chosen.

Consequence: `StudySession` route carries `cardIds: List<String>` (always pre-resolved). "Study Again (All)" navigates back to `PreviewStudySessionScreen` so selection runs fresh; "Study Again (Failed)" navigates directly to `StudySession` with `cardIds = [failedCardIds]`, bypassing `PreviewStudySessionScreen` since the pool is already known.

`PreviewStudySessionScreen` also receives `filterTagIds: List<String>` (default empty). When non-empty, selection filters to cards carrying any of those Tags before randomization/scoring. Only populated by Subcategory Details "Start Session" when the user has active Tag chips. Algorithm order: fetch all cards for scope → filter by `filterTagIds` if non-empty → randomize/score → pick top N.
