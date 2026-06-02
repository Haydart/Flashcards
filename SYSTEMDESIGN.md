# Flashcards App — System Design

## Navigation

3 top-level tabs:
```
🏠 Home · 📚 Study · ⚙️ Settings
```

### Nav graph structure

```
Root NavHost
├── Splash
├── Login
└── AuthedGraph  ← all auth-required screens; popUpTo<AuthedGraph>(inclusive=true) on sign-out. See ADR-0002.
    ├── Main  ← bottom nav shell
    │   ├── HomeGraph (nested graph)
    │   │   ├── HomeRoot
    │   │   ├── HomeCategoryDetails(categoryId)
    │   │   └── HomeSubcategoryDetails(categoryId, subcategoryId)
    │   ├── StudyGraph (nested graph)
    │   │   ├── StudyRoot
    │   │   ├── StudyCategoryDetails(categoryId)        ← added when Study tab is built
    │   │   └── StudySubcategoryDetails(categoryId, subcategoryId)
    │   └── SettingsGraph (nested graph)
    │       └── SettingsRoot
    ├── PreStartScreen(categoryId, subcategoryIds, filterTagIds)
    ├── StudySession(categoryId, subcategoryIds, cardIds)
    ├── SessionSummary
    └── CreatePrivateFlashcard(subcategoryId)
```

Screens in `AuthedGraph` outside `Main` are full-screen (no bottom nav visible).

### Shared screens (CategoryDetails, SubcategoryDetails)

Accessible from both Home and Study tabs. Use **tab-prefixed route types** (`HomeCategoryDetails` / `StudyCategoryDetails`) so each tab maintains an independent back stack with save/restore. Both route types call the same composable function — no UI duplication. See [ADR-0003](docs/adr/0003-tab-prefixed-shared-routes.md).

### Session entry routing

All session entry points navigate to `PreStartScreen`, which owns card selection from the given scope. See [ADR-0004](docs/adr/0004-pre-start-screen-card-selection.md).

- **Study Again (All)**: → `PreStartScreen` with same params, `popUpTo<Main>()`.
- **Study Again (Failed)**: → `StudySession` directly with `cardIds = [failedCardIds]`, `popUpTo<Main>()`.
- **Back to Home / system back from SessionSummary**: `popUpTo<Main>(inclusive = false)` — returns to whatever tab was active.

### Cross-tab navigation

Home empty state CTA ("Start your first session") triggers a tab switch to Study via callback wired in `MainScreen`. No cross-NavController state sharing.

## Home Screen

- Greeting with user's display name
- **Recents** carousel — past Study Sessions, two card variants:
  - *Single-subcategory*: shows Subcategory + Category name; taps into Subcategory Details
  - *Composite*: shows Category name only; taps into Category Details
- **Favorites** carousel — bookmarked Subcategories; each card shows Subcategory + Category name; taps into Subcategory Details
- Sections with no content are hidden individually
- Empty state (both carousels empty): illustration + "Start your first session" CTA → navigates to Study tab

## Study Screen

- Scrollable list of all top-level Categories as UI cards (default state)
- Each Category card shows:
  - Category name
  - Preview row of Subcategories (labeled **Topics** in the UI)
  - Total Topic count (e.g. "12 topics")
- Search bar filters in real time, matching against **Category name and Subcategory name only** (no Tags, no Flashcard content)
- Min query length: 2 characters — shorter queries show the default Category list
- Search results render in two sections:
  - **Topics** (shown first) — one compact row per matched Subcategory; row shows `Subcategory · Category` breadcrumb; tapping → Subcategory Details (single-subcategory intent)
  - **Categories** (shown second) — Category cards in their normal shape; tapping → Category Details
- A Category card appears in the Categories section when either its name matches or any of its Subcategories match
- Category card preview row in search results shows the default first-N Subcategories (matched Topics already surface as Topic rows above — no duplication)
- Sort order: Topics by match quality (exact > prefix > substring, tiebreak alphabetical); Categories by Firestore `order` field
- No-match empty state: illustration + copy quoting the query (e.g. "No topics or categories match 'xyz'")
- No tag-based filtering; search only

## Category Details Screen

- Lists all Subcategories (labeled **Topics**) for the Category
- Three session-start options:
  - **Quick Session** button — system auto-selects Subcategories → Pre-start Screen → composite Study Session begins
  - **Start Composite Session** button — transforms the list into multi-select mode; user selects Topics; "Start" button becomes active after ≥1 selected → Pre-start Screen → composite Study Session begins
  - **Fast-start action on each Topic row** — routes directly to Pre-start Screen for that Subcategory (skips Subcategory Details), without navigating away from Category Details
- Tapping a Topic row (not its fast-start action) → Subcategory Details screen

## Subcategory Details Screen

- Lists all Flashcards belonging to the Subcategory
- App bar includes:
  - **"Start Session"** button → Pre-start Screen with `filterTagIds` = currently active Tags → single-subcategory Study Session begins
  - **Filter icon button** (top-right) — opens the Tag filter dialog; shows a dot badge when ≥1 Tag is active
- **Tag filter dialog** (modal overlay, no Apply button — selections apply on close):
  - Header row: **"Select All"** and **"Unselect All"** actions, separated from the tag list by a divider
  - Staggered grid of tag chips below the divider: all Specific Tags + the "private" System Tag for this Subcategory
  - Multi-select with **OR** semantics — active Tags filter both the visible Flashcard list and the Study Session pool
  - Common Tags never appear
- FAB → create Private Flashcard

## Pre-start Screen

Full-screen modal that precedes every Study Session. Receives `categoryId`, `subcategoryIds`, and `filterTagIds: List<String>` (empty by default). Displays session scope summary: card count, topic count, estimated duration. Below the stats row: pill-button radio group for **Study Mode** selection (Rated | Fast), with a short description shown beneath the selected pill. Default: Rated. "Start session" button launches the session with the selected mode. Future: re-randomize button, card count slider.

This is the only place Study Mode is chosen.

**Card selection algorithm (runs on Pre-start Screen):**
1. Fetch all Flashcards for the given `subcategoryIds`
2. If `filterTagIds` is non-empty: filter to cards where `card.tags` intersects `filterTagIds` (OR semantics — a card qualifies if it carries any of the active Tags)
3. Apply scoring and selection (MVP: random shuffle; target: performance-weighted sort → pick top N)
4. Pass resolved `cardIds` to `StudySession`

`filterTagIds` is only ever non-empty for single-subcategory sessions launched from Subcategory Details. All other entry points (Quick Session, fast-start, Composite) pass `filterTagIds = emptyList()`.

## Study Session Flow

A session is scoped to one Category and one or more Subcategories. Card selection and Study Mode are determined on the Pre-start Screen before the session begins. If the available Flashcard pool is smaller than the configured session size N, the session starts with however many Flashcards exist — no warning shown.

**Study Modes:**
- **Rated**: user reveals each answer manually, then self-rates (Failed / Partial / Correct). Terminal State cards written to Firestore.
- **Fast**: cards advance automatically on a timer — question shown for a fixed window, answer auto-revealed, then next card. No Ratings, no Attempts, no Terminal States. Only session metadata written to Firestore.

### Flashcard Mechanics

**Bottom sheet (persistent, fixed height, no scrim):**
- **Question state**: single "Show answer" CTA centered in the sheet
- **Answer state**: "How well did you know it?" label + **Failed · Partial · Correct** buttons
- Transition between states: fade cross-dissolve (~150ms)

**Card reveal (question → answer):**
- Triggered by tapping "Show answer" in the sheet or swiping up on the card
- Card expands downward; question text shrinks and animates upward
- Answer content fades in and shoots up into view
- QUESTION / ANSWER labels change color on reveal
- Overflowing content (long text, code blocks) scrolls inside the card; bottom sheet stays pinned

**Attempt label:** "Attempt X of 3" visible in both question and answer states

**Progress indicator:** **"X/N mastered"** — X increments immediately on "Correct" tap (before card transitions away); N = initial Flashcard count

**After rating:** auto-advances immediately to next card — no "Next" button. Transition: current card slides left + scales down + fades out; next card slides in from right + scales up + fades in. Sheet fades back to "Show answer" state for the new card.

**Exit:** X button (top-left) only — no in-session "Finish session" button. X always shows a confirmation dialog warning that session progress will be lost.

### Re-insertion Rules

- **Correct** (any Attempt): Flashcard exits queue → Mastered, X increments
- **Partial** or **Failed**: Flashcard re-inserted with Attempt count incremented
- Each Flashcard has a maximum of 3 Attempts
- On 3rd Attempt: Correct → Mastered; Partial or Failed → Terminal State = Failed

### Session Termination

- **Natural end**: queue empties (all Flashcards Mastered or exhausted 3 Attempts) — Terminal State Flashcards written to Firestore, session written to Recents
- **Premature exit** (X button → confirm dialog): Terminal State Flashcards reached so far are written to Firestore, session written to Recents; Flashcards still in the queue are treated as unseen
- App kill during session: session is lost, no data saved, no resumption

### Data Saving

- Only Terminal State Flashcards are written to `progress/{cardId}`
- Each write includes a timestamp (required for future spaced-repetition scheduling)
- Individual review events written to `progress/{cardId}/reviews/` subcollection
- Session metadata written to `recentSessions/{sessionId}`

### Session Summary Screen

- **Study Again (All)** — navigates to Pre-start Screen with same `categoryId` + `subcategoryIds`, clearing the session stack (`popUpTo<Main>()`); card re-selection happens fresh on the Pre-start Screen
- **Study Again (Failed)** — shown only if ≥1 Flashcard reached Terminal State Failed; navigates directly to `StudySession` with `cardIds = [failedCardIds]`, `popUpTo<Main>()`
- **Back to Home** — `popUpTo<Main>(inclusive = false)`; returns to Main on whichever tab was active. System back has the same behavior.

## Settings Screen

- App preferences: session Flashcard count (default 20), etc.
- Permissions
- Voice settings (deferred, not MVP)
- Not for Category or Subcategory management

## Firestore Schema

```
// Global taxonomy (admin-defined)
categories/{catId}                           → { name, parentId, order }
tags/{tagId}                                 → { name, subcategoryIds[], type: "specific"|"common" }
cards/{cardId}                               → { question, answer, subcategoryId, tags[], createdAt }

// Per-user
users/{uid}/favorites/{subcategoryId}        → { createdAt }
users/{uid}/recentSessions/{sessionId}       → { categoryId, subcategoryIds[], completedAt }
users/{uid}/progress/{cardId}                → { failedCount, correctCount, lastReviewedAt, nextReviewAt }
users/{uid}/progress/{cardId}/reviews/{id}   → { rating, reviewedAt }
users/{uid}/privateCards/{cardId}            → { question, answer, subcategoryId, tags[], status, createdAt }
users/{uid}/privateCards/{cardId}/reviews/   → { rating, reviewedAt }
```

- **Strict 2-level taxonomy**: a Subcategory is a Category document with a non-null `parentId` pointing to a top-level Category. No deeper nesting; in-Subcategory grouping is done via specific Tags surfaced as filter chips on Subcategory Details. See [ADR-0001](docs/adr/0001-flat-two-level-taxonomy.md). Admin-defined globally.
- Tags are predefined globally (MVP). No user-created tags in MVP.
- Private Flashcard `status`: `"private" | "submitted" | "approved"` — promotion pipeline to global pool.
- Offline: Firestore Android SDK built-in persistence. No Room needed.
- Partial Rating is an in-session mechanic only; never written to Firestore as a standalone status.

## Flashcard Selection Algorithm

Client-side only. Pure logic on Firestore data:

- Score per Flashcard: `(failedCount / (failedCount + correctCount)) * recencyWeight`
- Filter: exclude cards where `nextReviewAt > now`
- Sort by score descending → pick top N → slight shuffle

Session Flashcard count: user-configurable in Settings, default 20.

Progress storage — hybrid:
- Aggregates on `progress/{cardId}` doc (fast reads for scoring)
- Individual review events in `progress/{cardId}/reviews/` subcollection (analytics, future ML)

## Private Flashcards

Created from Subcategory Details screen via FAB. Fields:
- Question (multiline text)
- Answer (multiline text)
- Tags (multi-select from predefined Specific Tags for this Subcategory):
  - Opens with all tags unchecked — no filter state propagated from the Subcategory Details screen
  - "General" tag is not shown; if user submits with no tags checked, "General" is auto-assigned (intentional friction against unclassified cards)
  - "private" System Tag is not shown; always auto-applied to every Private Flashcard regardless of user selection

Saved to `users/{uid}/privateCards/`. Future: admin promotes to global pool if quality sufficient.

## Content Seeding

Python script using Firebase Admin SDK:
- Reads JSON fixture files
- Idempotent upsert: write if not exists, skip if already present (match on stable ID)
- Supports extending existing structure without wiping — enables third-party tools to produce new questions

Fixture JSON format:
```json
{
  "categories": [{ "id": "android", "name": "Android", "parentId": null }],
  "subcategories": [{ "id": "compose", "name": "Compose", "parentId": "android" }],
  "tags": [{ "id": "ui", "name": "UI", "subcategoryIds": ["compose"], "type": "specific" }],
  "cards": [{ "id": "...", "question": "...", "answer": "...", "subcategoryId": "compose", "tags": ["ui"] }]
}
```

## Design System

Branded Material 3 — full M3 `ColorScheme` kept intact, with an additive `BrandColors` layer for app-specific colors. See [ADR-0005](docs/adr/0005-branded-m3-design-system.md).

**Color access in composables:**
- `MaterialTheme.colorScheme.*` — M3 roles (surfaces, primary, secondary, error, etc.)
- `MaterialTheme.brandColors.*` — branded extras (gradients as `Brush`, difficulty tints, etc.)

`BrandColors` slots are added incrementally as screens need them. Do not read `Color.kt` tokens directly in composables.

## Not in MVP

- Push notifications / study reminders (needs FCM + WorkManager)
- Voice answer evaluation (backend feature, planned)
- Custom user-created tags
- Admin UI for content management
- Gamification (streaks, XP)
- Performance-weighted Quick Session (MVP uses random selection)
- Session parameters (Flashcard count override, difficulty, time limit)
