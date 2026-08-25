# Flashcards App — System Design

## Navigation

4 top-level tabs:
```
🏠 Home · 📚 Study · 📊 Progress · ⚙️ Settings
```

### Nav graph structure

```
Root NavHost
├── Splash
├── Login
└── AuthedGraph  ← all auth-required screens; popUpTo<AuthedGraph>(inclusive=true) on sign-out. See ADR-0002.
    ├── Main  ← bottom nav shell
    │   ├── HomeGraph (nested graph)
    │   │   └── HomeRoot
    │   ├── StudyGraph (nested graph)
    │   │   └── StudyRoot
    │   ├── ProgressGraph (nested graph)
    │   │   └── ProgressRoot
    │   └── SettingsGraph (nested graph)
    │       └── SettingsRoot
    ├── CategoryDetails(categoryId, categoryName)                    ← full-screen, no bottom nav; shared by Home + Study
    ├── SubcategoryDetails(categoryId, categoryName, subcategoryId, subcategoryName)  ← full-screen, no bottom nav; shared by Home + Study
    ├── PreviewStudySession(categoryId, categoryName, subcategoryIds, subcategoryNames, filterTagIds, isQuickSession)
    ├── StudySession(categoryId, sessionTitle, subcategoryIds, cardIds, studyMode, voiceAnsweringEnabled)
    ├── SessionSummary  ← route type exists (`StudySummaryRoute`), never registered in the nav graph — see Session Summary Screen
    └── CreatePrivateFlashcard(subcategoryId)
```

Screens in `AuthedGraph` outside `Main` are full-screen (no bottom nav visible).

### Shared screens (CategoryDetails, SubcategoryDetails)

Accessible from both Home and Study tabs. Registered at the root NavHost level (siblings of Main), so they appear full-screen with no bottom navigation bar. A single route type serves both ingresses — navigation uses the root NavController passed down through MainScreen callbacks.

### Session entry routing

All session entry points navigate to `PreviewStudySessionScreen`, which owns card selection from the given scope. See [ADR-0004](docs/adr/0004-preview-study-session-screen-owns-card-selection.md).

- **Study Again (All)**: → `PreviewStudySessionScreen` with same params, `popUpTo<Main>()`.
- **Study Again (Failed)**: → `StudySession` directly with `cardIds = [failedCardIds]`, `popUpTo<Main>()`.
- **Back to Home / system back from SessionSummary**: `popUpTo<Main>(inclusive = false)` — returns to whatever tab was active.

### Cross-tab navigation

Home empty state CTA ("Start your first session") triggers a tab switch to Study via callback wired in `MainScreen`. No cross-NavController state sharing.

## Home Screen

- Greeting with user's display name
- **Recents** carousel — past Study Sessions, two card variants (**designed, not yet built**: no `recentSessions` write, no query, no carousel code exists in `feature/home` today — see Session Termination):
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
  - **Quick Session** button — system auto-selects Subcategories → Preview Study Session Screen → composite Study Session begins
  - **Start Composite Session** button — transforms the list into multi-select mode; user selects Topics; "Start" button becomes active after ≥1 selected → Preview Study Session Screen → composite Study Session begins
  - **Fast-start action on each Topic row** — routes directly to Preview Study Session Screen for that Subcategory (skips Subcategory Details), without navigating away from Category Details
- Tapping a Topic row (not its fast-start action) → Subcategory Details screen

## Subcategory Details Screen

- Lists all Flashcards belonging to the Subcategory as collapsible items (collapsed: question + tag chips; expanded: + answer)
- App bar: back navigation + bookmark action + overflow menu (placeholder today for a near-future dynamic launcher shortcut item, see `docs/design/launcher-shortcuts.md`)
- **Bottom toolbar** (always visible, floating): Filter icon, Sort icon, Add(+) icon (create Private Flashcard), then a trailing extended-pill FAB, "Start Session" → Preview Study Session Screen with `filterTagIds` = currently active Tags → single-subcategory Study Session begins. See [ADR-0022](docs/adr/0022-subcategory-details-filter-sort-toolbar.md).
  - **Filter** — single entry point opening one sheet with Tags (multi-select, OR semantics, chips derived `distinct(card.tags)` plus a **"Private"** chip) and a Difficulty `RangeSlider` (1–10); the two facets AND-combine. Active Tags/difficulty filter both the visible Flashcard list and the Study Session pool. Badge dot when non-default.
  - **Sort** — menu with **Default | Easiest first | Hardest first**, mutually exclusive; reorders the visible list only, never changes which/how many cards are shown. Badge dot when non-default.
- Filtering to zero results is a designed-but-not-yet-implemented empty state (clear-filters action, Start-session FAB disabled).

## Preview Study Session Screen

Full-screen modal that precedes every Study Session. Receives `categoryId`, `categoryName`, `subcategoryIds`, `subcategoryNames`, `filterTagIds: List<String>` (empty by default), and `isQuickSession: Boolean` (false by default). A read-only hero shows session scope: card count, estimated duration, and topic count (multi-topic sessions only). Below it, a **plain column** (not yet the sheet chrome below) presents each adjustable setting as a summary row — each shows its current value and opens a focused `FlashcardsDialog`-based dialog. Rows and visibility, as actually built:

- **Mode** — Rated | Fast (default Rated). Always shown.
- **Voice answering** — On | Off. Rated sessions only.
- **Length** — session card count. Always shown.
- **Filters** — merged tags + difficulty (ADR-0022 shape; tags OR-within, AND-combined with a difficulty range). Tag facet appears for single-subcategory sessions only; multi-topic sessions filter by difficulty only. Always shown.
- **Sort** — Default | Easiest first | Hardest first. Controls presentation order of the already-selected cards within the session (Easiest/Hardest reorder by difficulty; Default is the selection algorithm's natural output order). Does not influence which cards get selected — that's the card selection algorithm below, unaffected by this row. Always shown.

**Not yet built (ADR-0030 designs both):** a persistent no-scrim bottom-sheet chrome wrapping these rows, and a separate **Voice** row (TTS voice + speed, shown for Fast or Rated+voice-answering-on) — today that setting is reachable only from the session cog and the Settings screen.

Each edit popup carries a "keep as default" checkbox (persists a global default; unchecked is session-scoped), except the Filters popup (tags + difficulty always session-scoped) — though note the persistence write itself is a `TODO`, see Settings Screen below. "Start session" launches the session with the chosen settings; a "Re-randomize" button (multi-topic and Quick sessions only) re-runs card selection over the same pool with a new seed. See [ADR-0030](docs/adr/0030-preview-session-settings-sheet.md) and [docs/design/study-session-preview-sheet.md](docs/design/study-session-preview-sheet.md).

This is the only place Study Mode (and, up front, Voice answering) is chosen for a concrete session — onboarding and the Settings screen only set the persisted default preference, neither starts a session. The "keep as default" checkbox on each Preview popup (see above) is what lets this screen also update that persisted default, without leaving session scope.

**Card selection algorithm (runs on Preview Study Session Screen):**
1. Fetch all Flashcards for the given `subcategoryIds`
2. If `filterTagIds` is non-empty: filter to cards where `card.tags` intersects `filterTagIds` (OR semantics — a card qualifies if it carries any of the active Tags)
3. Filter to cards where `card.difficulty` falls within the selected difficulty range (inclusive on both ends), AND-combined with the tag filter
4. Apply scoring and selection (MVP: random shuffle; target: performance-weighted sort → pick top N — see "Flashcard Selection Algorithm" below for the scoring formula)
5. Pass resolved `cardIds` to `StudySession`; presentation order within the session is then set independently by the Preview screen's Sort row (Default/Easiest/Hardest), not by this selection step

`filterTagIds` is only ever non-empty for single-subcategory sessions launched from Subcategory Details. All other entry points (Quick Session, fast-start, Composite) pass `filterTagIds = emptyList()`.

## Study Session Flow

A session is scoped to one Category and one or more Subcategories. Card selection and Study Mode are determined on the Preview Study Session Screen before the session begins. If the available Flashcard pool is smaller than the configured session size N, the session starts with however many Flashcards exist — no warning shown.

**Study Modes:**
- **Rated**: user reveals each answer manually, then self-rates (Failed / Partial / Correct). Terminal State cards written to Firestore.
- **Fast**: cards advance question → reveal answer → next, manually (tap to reveal, tap to advance) by default, or with read-aloud enabled: system TTS reads the question (`questionSpoken` if present, else `question`), pauses 1 500 ms, reads the answer (`answerSpoken` if present, else `answer`), pauses 2 500 ms, then auto-advances. When read-aloud is on, user controls: pause/play, skip-next, skip-previous, speech-rate slider (0.5×–2×), Show Answer (interrupts question, reads answer immediately). Playback persists with screen off or app backgrounded via `StudySessionVoiceService` (foreground `mediaPlayback` service). Persistent `MediaStyle` notification + lock-screen controls via `MediaSessionCompat`. No Ratings, no Attempts, no Terminal States. Firestore session metadata write deferred. See [ADR-0012](docs/adr/0012-tts-mediasession-stack-for-fast-mode.md).

**Fast mode entry point:** Study Mode is chosen exclusively on the Preview Study Session Screen (ADR-0004). Read-aloud is an opt-in toggle on that screen (Voice row); when on, a session routed with `studyMode = FAST` auto-starts voice playback once its cards are loaded (requesting the notification permission first on Android 13+), otherwise the session is manual tap-to-reveal/advance. Voice is tied to the session screen lifetime; navigating away stops playback. A confirmation dialog on session exit is planned.

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

**Attempt label:** "Attempt X of N" visible in both question and answer states (N = user's configured Attempts limit, Settings — default 3, max 5) — planned, not yet implemented

**Progress indicator:** designed as **"X/N mastered"** (X incrementing on "Correct" tap) — **not implemented**; the top app bar today shows a plain "current / total" card-index counter (e.g. "3 / 18"), unrelated to Rating outcome

**After rating:** auto-advances immediately to next card — no "Next" button. Transition: current card slides left + scales down + fades out; next card slides in from right + scales up + fades in. Sheet fades back to "Show answer" state for the new card.

**Exit:** X button (top-left) only — no in-session "Finish session" button. X always shows a confirmation dialog warning that session progress will be lost.

**Flag icon:** flag `IconButton` in the study session **top app bar**, left of the "3/18" card counter (not on the card itself — the bottom sheet is already crowded with mic, cog and transport controls). Shown only while a card is displayed. Tapping opens the **"Report a problem"** dialog (`FlashcardsDecisionDialog`): all 7 Curation Actions as independently-toggleable checkboxes — Raise the difficulty, Lower the difficulty, Wrong tags, Needs a code example, Formatting looks broken, Needs a full rewrite, Duplicate or low quality — plus Cancel/Submit. The draft always starts empty (never seeds from the card's existing report); Submit upserts the checked Curation Actions to `users/{uid}/curationRequests/{cardId}` in one write. **No withdrawal path exists** — unchecking a row no longer removes anything from a previous submission, it only affects the current draft; Cancel discards the draft. The card continues in the session queue — no suppression. See [ADR-0017](docs/adr/0017-curation-report-system.md).

### Re-insertion Rules (planned, not yet implemented — current code advances to next card on any Rating, no re-insertion or Attempt tracking)

- **Correct** (any Attempt): Flashcard exits queue → Mastered, X increments
- **Partial** or **Failed**: Flashcard re-inserted with Attempt count incremented
- Each Flashcard has a maximum of N Attempts (user-configurable in Settings, default 3, max 5)
- On the Nth (final) Attempt: Correct → Mastered; Partial or Failed → Terminal State = Failed

### Session Termination

**Not yet implemented — this whole section is target design.** Today `onRating()` does nothing but advance to the next card (no re-insertion, no Attempt tracking — see Flashcard Mechanics above), and both natural end and premature exit resolve to the same `onNavigateBack()` call with **no Firestore write of any kind**. `isSessionComplete` just triggers that pop; nothing downstream of it exists yet.

Designed:
- **Natural end**: queue empties (all Flashcards Mastered or exhausted their Attempts limit) — Terminal State Flashcards written to Firestore, session written to Recents
- **Premature exit** (X button → confirm dialog): Terminal State Flashcards reached so far are written to Firestore, session written to Recents; Flashcards still in the queue are treated as unseen
- App kill during session: session is lost, no data saved, no resumption

### Data Saving

**Not yet implemented** — none of these writes exist in code today; kept here as target design once Re-insertion Rules and Session Termination land.

- Only Terminal State Flashcards are written to `progress/{cardId}`
- Each write includes a timestamp (required for future spaced-repetition scheduling)
- Individual review events written to `progress/{cardId}/reviews/` subcollection
- Session metadata written to `recentSessions/{sessionId}`

### Session Summary Screen

**Not yet implemented.** `StudySummaryRoute` exists as a route type but is never registered in the nav graph and no screen composable exists for it. Today, session end (natural or premature) just calls `onNavigateBack()` straight to whichever tab was active — no summary screen in between, so none of the bullets below happen yet.

Designed:
- **Study Again (All)** — navigates to Preview Study Session Screen with same `categoryId` + `subcategoryIds`, clearing the session stack (`popUpTo<Main>()`); card re-selection happens fresh on the Preview Study Session Screen
- **Study Again (Failed)** — shown only if ≥1 Flashcard reached Terminal State Failed; navigates directly to `StudySession` with `cardIds = [failedCardIds]`, `popUpTo<Main>()`
- **Back to Home** — `popUpTo<Main>(inclusive = false)`; returns to Main on whichever tab was active. System back has the same behavior.

## Settings Screen

**Still a scratch screen** — `SettingsScreen.kt` renders `Text("Settings - NYI")` plus a Showkase entry point and sign-out; none of the sections below are built. Listed here as target design.

**Study Sessions**
- Session Flashcard count (default 20)
- Attempts per card (default 3, max 5, user-configurable — planned, not yet implemented)
- Default study mode (Rated | Fast) — same persisted preference the "keep as default" checkbox on the Preview Study Session Screen's Mode popup writes to (ADR-0030); also set during onboarding
- Default sort order (Default | Easiest | Hardest) — mirrors the Preview screen's Sort "keep as default" checkbox

**None of the three "keep as default" checkboxes above have anywhere to write to yet** (dialog-system Gap 1): no `StudyPreferences` model, no repository, no `SetDefault*` use cases, no DataStore file exist. The checkboxes render and travel into each dialog's draft, but committing them is a `TODO(dialog-system Gap 1)` in `PreviewStudySessionViewModel.onDialogConfirm`.

**Voice**
- Voice settings (voice selection + playback speed — implemented, `VoiceSettingsDialog`, persisted via `DataStoreVoiceSettingsLocalDataSource`)
- Voice answering **consent** (has the user accepted the mic-permission disclosure) — implemented and persisted, `VoiceAnswerConsentRepository`/`SetVoiceAnswerConsentUseCase`. Premium-gated with real server-side entitlement enforcement (ADR-0024/0029).
- Voice answering **default-enabled** — not persisted; also part of the missing `StudyPreferences` (Gap 1) alongside Mode/Length/Sort above, despite living in a different section here
- Read-aloud/auto-play default (Fast) — likewise not persisted, same Gap 1

**Notifications**
- Daily reminder toggle (row exists; backend — FCM + WorkManager — not yet scoped/built)

**Permissions**
- Notifications, Microphone — status + link to system settings

**Daily Goal**
- Minutes/day (default 20) — also editable inline on the Progress screen; both write the same persisted value

**Account**
- Sign out

Not for Category or Subcategory management.

## Report a Problem (in-session flag icon)

Reached via the flag icon in the study session top app bar (Rated and Fast alike), enabled only while a card is displayed. Ships to production — no debug gate. Lets a user flag the current Flashcard for a specific content fix without leaving the session.

- **Dialog**: `ReportProblemDialog`, a `FlashcardsDecisionDialog`. Lists all 7 Curation Actions with icon + label ("Raise the difficulty," "Lower the difficulty," "Wrong tags," "Needs a code example," "Formatting looks broken," "Needs a full rewrite," "Duplicate or low quality") — exhaustive by a `check()` against `CurationAction.entries`, so a new action can't be added without a row. The draft always starts **empty**, regardless of the card's existing report — it never seeds from Firestore. Cancel discards the whole draft; Submit is enabled once ≥1 row is checked.
- **Writes**: on Submit — the checked Curation Actions are upserted to Firestore in one write (`SubmitCurationReportUseCase` → `CurationRepository.upsertCurationActions`). Cancel makes no write.
- **No withdrawal path**: because the draft never seeds, there is no way to un-report a previously submitted action from within the app. Withdrawal, if ever needed, is admin/sync-tooling operating on Firestore directly.
- **Resubmission is a no-op**: `DefaultCurationRepository` caches each card's last known flagged set, lazily fetched from Firestore on that card's first write; upserting a set that's already a subset of it skips the write instead of re-flagging on every reopen of the dialog.
- **No management screen**: Curation Requests are consumed only by admin sync scripts, never surfaced back to the user.

See [ADR-0017](docs/adr/0017-curation-report-system.md).

## Firestore Schema

```
// Global taxonomy (admin-defined)
categories/{categoryId}                               → { name, order, subcategoryCount, iconUrl }
subcategories/{categoryId-subSlug}                    → { name, categoryId, order, cardCount }
subcategories/{categoryId-subSlug}/flashcards/{cardId} → { question, answer, tags[], difficulty,
                                                           createdAt,
                                                           questionCode?, answerCode?,
                                                           extendedContext?,
                                                           questionSpoken?, answerSpoken? }

// Per-user
users/{uid}/favorites/{subcategoryId}                 → { createdAt }
// Not yet written anywhere in code — see Session Termination / Data Saving above.
users/{uid}/recentSessions/{sessionId}                → { categoryId, categoryName,
                                                          subcategoryIds[], subcategoryNames[],
                                                          completedAt, cardCount, masteredCount?,
                                                          studyMode: "rated"|"fast" }
// Not yet written anywhere in code — see Session Termination / Data Saving above.
users/{uid}/progress/{cardId}                         → { failedCount, correctCount, lastReviewedAt, nextReviewAt }
users/{uid}/progress/{cardId}/reviews/{id}            → { rating, reviewedAt }
users/{uid}/privateCards/{subcategoryId}/flashcards/{cardId} → { question, answer, tags[], status, createdAt }

// Report a problem (in-session flag icon)
users/{uid}/curationRequests/{cardId}                       → { subcategoryId: String,
                                                               actions: {
                                                                 "<CurationAction>": { flaggedAt: Timestamp }
                                                               } }
// CurationAction values: DifficultyTooEasy | DifficultyTooHard | WrongTags |
//                        NeedsCodeExample | BacktickRedo | FullRedo | Delete
```

- **Strict 2-level taxonomy**: Categories and Subcategories are separate top-level collections. Subcategory IDs are namespaced `{categoryId}-{subSlug}` (e.g. `android-testing`) to guarantee uniqueness across parent categories. See [ADR-0001](docs/adr/0001-flat-two-level-taxonomy.md) and [ADR-0007](docs/adr/0007-firestore-collection-structure.md).
- **Cards live as a subcollection of their Subcategory** (`subcategories/{subcategoryId}/flashcards/`). Fetching all Flashcards for a Subcategory is a single `getDocuments()` — no WHERE clause, no index. No separate `cards/` collection. See [ADR-0007](docs/adr/0007-firestore-collection-structure.md).
- **`subcategoryId` is not stored on Flashcard documents** — it is encoded in the collection path.
- **`difficulty` is a mandatory integer (1–10)** on global Flashcard documents. Documents missing this field are filtered at the DTO layer and never reach the domain. Private Flashcards carry no `difficulty`. See [ADR-0010](docs/adr/0010-difficulty-field-design.md).
- **`extendedContext` is nullable** on global Flashcard documents. Omitted on simple cards (difficulty 1–3) where the Q&A is fully self-explanatory. Present and progressively richer as difficulty rises: mid cards (4–6) carry a concrete example or short snippet; hard/expert cards (7–10) carry fuller context — edge cases, cross-concept relationships, pitfalls. Never duplicates the `answer` field.
- **Tags are flat untyped strings** in `tags[]` on each Flashcard. No `tags/` collection. See [ADR-0006](docs/adr/0006-flat-denormalized-tags.md).
- **Category `iconUrl`**: absolute HTTPS URL. No Firebase Storage SDK dependency in UI layer.
- **`recentSessions` denormalizes names and stats** at write time — `categoryName`, `subcategoryNames[]`, `cardCount`, `masteredCount` — so the Home screen renders Recent cards from a single read per session. `masteredCount` omitted for Fast Study Sessions. **Not yet written** — see Session Termination.
- Private Flashcard `status`: `"private" | "submitted" | "approved"` — promotion pipeline to global pool.
- **`curationRequests/{cardId}` is a flat collection** keyed by globally-unique cardId. Stores structured content-fix directives raised by any user via the in-session "Report a problem" dialog, consumed by admin sync scripts — not surfaced back to users anywhere in the app. Actions are a map of `CurationAction` string → `{ flaggedAt }`. Doc is deleted when all actions are removed. See [ADR-0017](docs/adr/0017-curation-report-system.md).
- Offline: Firestore Android SDK built-in persistence. No Room needed.
- Partial Rating is an in-session mechanic only; never written to Firestore as a standalone status.

## Flashcard Selection Algorithm

**As actually implemented** (`SelectSessionFlashcardsUseCase`, `core:domain`) — client-side, pure logic over a pool fetched per subcategory and cached in memory for the ViewModel's lifetime:

1. Filter to cards where `card.difficulty` falls in the configured range
2. Filter to cards where `card.tags` intersects the configured tag set (OR-within; step is a no-op when the set is empty)
3. Shuffle with a seeded `Random(config.seed)`, then take the configured length
4. Apply the configured sort order to that drawn subset only — Default keeps shuffle order, Easiest/Hardest sort by difficulty. Sorting never changes which cards were drawn (see Preview Study Session Screen above, Sort row).

Session Flashcard count: user-configurable (Length row on Preview, default 20; a Settings-screen default exists in design only — not yet persisted, see Settings Screen / Gap 1).

**Previously documented here, never built, now dead:** a `progress/{cardId}`-scored spaced-repetition selector — `(failedCount / (failedCount + correctCount)) * recencyWeight`, excluding cards where `nextReviewAt > now`. No such scoring exists anywhere in the codebase, and `progress/{cardId}` itself is never written (see Data Saving above). "Not in MVP" below already lists a future performance-weighted selector as the intended successor to today's random draw — this section previously described that unbuilt selector as shipped, which it never was.

## Private Flashcards

Created from Subcategory Details screen via FAB. Fields:
- Question (multiline text)
- Answer (multiline text)
- Tags (multi-select from the Tags already present on this Subcategory's Flashcards, derived `distinct(card.tags)`):
  - Opens with all tags unchecked — no filter state propagated from the Subcategory Details screen
  - "General" tag is not shown; if user submits with no tags checked, "General" is auto-assigned (intentional friction against unclassified cards)
  - No "private" tag exists; the Private flag is implicit — the card lands in `users/{uid}/privateCards/{subcategoryId}/flashcards/{cardId}`, which is what surfaces it under the "Private" filter chip

Saved to `users/{uid}/privateCards/{subcategoryId}/flashcards/{cardId}`. Future: admin promotes to global pool if quality sufficient.

## Content Seeding

Two-stage Python tooling under `scripts/seed/` (Firebase Admin SDK). The intermediate
fixture is a **local, gitignored temp file — never committed**:

1. **`build_fixture.py`** — reads the curated capture inboxes (`~/.claude/flashcards/<cat>/<sub>/inbox.jsonl`),
   applies the inbox→`cards` projection (see Import mapping above), derives `categories`/`subcategories`
   from the slugs, and writes `scripts/seed/.tmp/fixture.json`. Deterministic; fails loudly on
   duplicate card ids or a subcategory slug colliding across two parent categories.
2. **`seed_firestore.py`** — globs `scripts/seed/.tmp/*.json` and upserts via Admin SDK.
   Idempotent upsert keyed on card `id`: `--skip-existing` default (write if absent, skip if present),
   `--overwrite` to force, `--dry-run` to report only. Categories land in `categories/{id}`; Subcategories land in `subcategories/{id}`. Credentials via `GOOGLE_APPLICATION_CREDENTIALS` (service-account JSON, gitignored).

- Supports extending existing structure without wiping — enables third-party tools to produce new questions.
- **Card `id` is the Firestore document key** and must be globally unique; the capture skill now mandates a
  cryptographically random hex suffix to prevent collisions (100 historical collisions were repaired pre-import).

Fixture JSON format (no `tags` section — tags are inline strings on each card):
```json
{
  "categories": [{ "id": "android", "name": "Android", "order": 0, "subcategoryCount": 1, "iconUrl": "" }],
  "subcategories": [{ "id": "android-compose", "name": "Compose", "categoryId": "android", "order": 0, "cardCount": 1 }],
  "cards": [{ "subcategoryId": "android-compose", "id": "...", "question": "...", "answer": "...", "tags": ["state"], "difficulty": 4 }]
}
```

Cards are written to `subcategories/{subcategoryId}/flashcards/{cardId}`. The `subcategoryId` field in the fixture drives the collection path and is not written as a document field.

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
- Session time limit (Flashcard count override + difficulty filter are now first-class on the Preview Study Session Screen — see ADR-0030)
