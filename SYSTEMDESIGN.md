# Flashcards App — System Design

## Navigation

3 top-level tabs:
```
🏠 Home · 📚 Study · ⚙️ Settings
```

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
  - **Quick Session** button — system auto-selects Subcategories and Flashcards (MVP: random; target: performance-weighted) → composite Study Session starts immediately
  - **Start Composite Session** button — transforms the list into multi-select mode; user selects Topics; "Start" button becomes active after ≥1 selected → composite Study Session begins
  - **Fast-start action on each Topic row** — starts a single-subcategory Study Session for that Subcategory immediately, without navigating away
- Tapping a Topic row (not its fast-start action) → Subcategory Details screen

## Subcategory Details Screen

- Lists all Flashcards belonging to the Subcategory
- **Filter chip row** below screen header (no row label): one chip per **specific Tag** present in this Subcategory
  - Chips support multi-select with **OR** semantics (selecting "State Management" + "Side Effects" matches Flashcards carrying either Tag)
  - Active chips filter both the visible Flashcard list and the single-subcategory Study Session selection pool
  - Common Tags (cross-Subcategory umbrellas) are not surfaced — they remain internal/AI-facing
- App bar includes **"Start Session"** button → single-subcategory Study Session begins (respects active Tag chip filter)
- FAB → create Private Flashcard (Q/A form) — details deferred

## Study Session Flow

A session is scoped to one Category and one or more Subcategories. Flashcards are selected and ordered by per-flashcard performance scores and recency weights. If the available Flashcard pool is smaller than the configured session size N, the session starts with however many Flashcards exist — no warning shown.

### Flashcard Mechanics

- Flashcard shows question
- User swipes or taps "Show Answer" → flip animation reveals answer
- Rating buttons: **Failed · Partial · Correct**
- Progress indicator: **"X/N mastered"** — X increments only on Correct; N = initial Flashcard count

### Re-insertion Rules

- **Correct** (any Attempt): Flashcard exits queue → Mastered, X increments
- **Partial** or **Failed**: Flashcard re-inserted with Attempt count incremented
- Each Flashcard has a maximum of 3 Attempts
- On 3rd Attempt: Correct → Mastered; Partial or Failed → Terminal State = Failed

### Session Termination

- **Natural end**: queue empties (all Flashcards Mastered or exhausted 3 Attempts) — Terminal State Flashcards written to Firestore, session written to Recents
- **Finish Session** (premature exit): Terminal State Flashcards reached so far are written to Firestore, session written to Recents; Flashcards still in the queue are treated as unseen
- App kill during session: session is lost, no data saved, no resumption

### Data Saving

- Only Terminal State Flashcards are written to `progress/{cardId}`
- Each write includes a timestamp (required for future spaced-repetition scheduling)
- Individual review events written to `progress/{cardId}/reviews/` subcollection
- Session metadata written to `recentSessions/{sessionId}`

### Session Summary Screen

- **Study Again (All)** — re-runs the session with the same Subcategory scope using freshly written scores
- **Study Again (Failed)** — re-drills only failed Flashcards from this session; shown only if ≥1 Flashcard failed
- **Back to Home**

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
- Tags (multi-select from predefined global tags for that Subcategory's parent Category)

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

## Not in MVP

- Push notifications / study reminders (needs FCM + WorkManager)
- Voice answer evaluation (backend feature, planned)
- Custom user-created tags
- Admin UI for content management
- Gamification (streaks, XP)
- Performance-weighted Quick Session (MVP uses random selection)
- Session parameters (Flashcard count override, difficulty, time limit)
