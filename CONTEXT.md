# Flashcards

A mobile app for studying curated flashcards organized by subcategory, with spaced-repetition-influenced flashcard selection.

## Language

### Entities

**Category**:
A top-level knowledge domain a user can study (e.g. Android, Python).
_Avoid_: Topic, Subject, Domain

**Subcategory**:
A granular area within a Category. A Flashcard belongs to exactly one Subcategory (e.g. Compose, Coroutines, Navigation under Android). Displayed to users as **Topic** in the UI; Subcategory is the canonical term in code and internal docs.
_Avoid_: Subtopic (the UI label "Topic" is intentional and not a synonym to avoid — it is the presentation-layer name)

**Tag**:
A global keyword carried by a Flashcard. One kind only — no type distinction, no internal/user-facing split; every Tag is user-facing and reusable across Subcategories (e.g. "State" may tag Flashcards in both Compose and Coroutines). Surfaced as filter chips on Subcategory Details, where the chip set is the Tags actually present on that Subcategory's Flashcards (derived, not an owned list). Tapping chips constrains both the browsed Flashcard list and the single-subcategory Study Session selection pool. Multi-select with OR semantics. A **General** Tag covers Flashcards with no narrower theme.
_Avoid_: Label, Topic, Filter (even though Tags filter, do not call the entity "Filter"); Specific Tag / Common Tag / System Tag (the typed-tag model is retired)

**Private flag**:
A derived boolean property: a Flashcard is Private iff it lives under `users/{uid}/privateCards/{subcategoryId}/flashcards/` (global `subcategories/{subcategoryId}/flashcards/` are never Private). Not a Tag. Surfaced as a "Private" filter chip on Subcategory Details (so users can study their private cards only) but absent from the New Flashcard tag selector.
_Avoid_: private Tag, System Tag

**Flashcard**:
A question-answer pair belonging to exactly one Subcategory. Part of the global admin-curated pool or a user's private collection.
_Avoid_: Card, Question

**Difficulty**:
A mandatory integer 1–10 on every global admin-curated Flashcard expressing how hard the question is within its Subcategory's domain. Domain-relative: a 3 in Compose and a 3 in Coroutines both mean "a beginner in that area gets this right." Global Flashcards with no Difficulty value are filtered out at the data layer and never reach the domain. Private Flashcards are exempt — they carry no Difficulty and are excluded from difficulty-aware features. Used by curriculum features to order Flashcards by complexity.
_Avoid_: Score, Level, Rank

**Extended Context**:
An optional supplementary payload on a global Flashcard providing self-contained teaching material — examples, code snippets, analogies, edge cases, and "why this matters" framing. Absent on simple cards (Difficulty 1–3) where the question and answer are already fully self-explanatory. Present and progressively richer as Difficulty rises: mid-range cards (4–6) include a concrete example or short snippet; hard/expert cards (7–10) include fuller context covering edge cases, cross-concept relationships, and pitfalls. Intended for a future "explain deeper" feature where an LLM re-teaches the concept from this payload. Never a duplicate of the answer field.
_Avoid_: Context, Explanation, Detail

**Private Flashcard**:
A Flashcard created by a user. Follows a submission lifecycle: `private → submitted → approved` (approved flashcards may be promoted to the global pool).
_Avoid_: User card, Custom card

**Favorite**:
A Subcategory explicitly bookmarked by a User. Displayed on the Home screen as a carousel of cards showing Subcategory and parent Category names.
_Avoid_: Starred, Saved, Liked

**Recent**:
A past Study Session surfaced on the Home screen as a carousel card. Renders as one of two variants: a **single-subcategory Recent** (session had one Subcategory — shows Subcategory + Category name, taps into Subcategory Details) or a **composite Recent** (session spanned multiple Subcategories — shows Category name only, taps into Category Details).
_Avoid_: History, Last session

**User**:
An authenticated person using the app. Represented in code as `AuthUser` with `uid`, `email`, `displayName`, `photoUrl`.
_Avoid_: Account, Player, Learner

**Study Session**:
A focused learning instance scoped to one or more Subcategories within a single Category. Has exactly one **Study Mode**. Fast Study Sessions write only session metadata to Firestore (no card progress). Rated Study Sessions write Terminal State cards to Firestore. A session with exactly one Subcategory is a **single-subcategory session**; a session spanning multiple Subcategories is a **composite session**.
_Avoid_: Quiz, Session alone (ambiguous with auth session)

**Study Mode**:
The interaction mechanic of a Study Session. Two values:
- **Rated**: user reveals each answer manually, then self-rates (Failed / Partial / Correct) — or enables **Voice Answering** in-session for hands-free listen-and-grade instead. Flashcards reaching a Terminal State are written to Firestore.
- **Fast**: cards advance question → reveal answer → next, either manually (tap to reveal, tap to advance) or with read-aloud enabled — system TTS reads the question aloud, pauses, reads the answer aloud, then auto-advances hands-free. User controls playback via transport controls (pause / play / skip / speed slider) when read-aloud is on. Playback continues with the screen off or app backgrounded. No Ratings, no Attempts, no Terminal States.
_Avoid_: Automatic mode, Passive mode, Browse mode, Voice mode (voice is the delivery mechanism, not the mode name)

**Attempt**:
A single presentation of a Flashcard to the user within a **Rated** Study Session. Each Flashcard has a maximum number of Attempts per session, user-configurable in Settings (default 3, max 5). Does not apply to Fast Study Sessions.
_Avoid_: Turn, Round, Try

**Rating**:
The user's self-assessment after viewing an answer in a **Rated** Study Session. Values: **Failed**, **Partial**, **Correct**. Produced either by a manual self-rating tap or by **Voice Answering**'s automatic grade — both write identically. A Partial or Failed Rating triggers re-insertion of the Flashcard into the session queue; Correct ends the Flashcard's Attempts. Does not apply to Fast Study Sessions.
_Avoid_: Score, Grade, Answer, Response

**Voice Answering**:
A Rated-Study-Sessions-only mechanic that replaces manual reveal-and-self-rate with hands-free listen-transcribe-grade: the shared Fast-mode TTS engine reads the question, the app listens for a spoken answer, transcribes and grades it, and the resulting grade band becomes the Flashcard's **Rating** exactly as a manual tap would. Off by default. Selectable up front as a row on the **Preview Study Session Screen**'s settings sheet (ADR-0030), and still toggleable in-session after the session has started. Enabling it auto-enables question TTS through the same engine Fast mode uses, but stops after the question — it never auto-progresses to reading the answer.
_Avoid_: Voice mode (voice is the delivery mechanism, not a Study Mode — see Study Mode's avoid list), Voice grading (grading is the mechanism inside the feature, not the feature's name)

**Terminal State**:
A Flashcard's final outcome in a **Rated** Study Session. A Flashcard reaches a Terminal State when it receives a Correct Rating (any Attempt) or exhausts its configured Attempts limit without a Correct Rating. Terminal states written to Firestore: **Mastered** (Correct) or **Failed** (not Correct within the limit). Partial on the final Attempt resolves to Failed. Does not apply to Fast Study Sessions.
_Avoid_: Final state, End state, Result

**Mastered**:
The Terminal State of a Flashcard that received a Correct Rating within a **Rated** Study Session.
_Avoid_: Completed, Passed, Correct (Correct is the Rating that causes Mastered, not a synonym)

**Curation Request**:
A user-submitted signal that a global Flashcard needs a specific content fix, raised via the in-session flag icon's **"Report a problem"** sheet (Rated and Fast alike). Stored at `users/{uid}/curationRequests/{cardId}`. One document per card; multiple Curation Actions can be active simultaneously, each independently toggleable — unchecking an action withdraws it, and the document is deleted once the last action is removed. No suppression — a flagged Flashcard still appears in Study Sessions. No management/withdraw screen exists; withdrawal happens only by reopening the report sheet on that card. Consumed by admin sync scripts. See [ADR-0017](docs/adr/0017-curation-report-system.md).
_Avoid_: Flag, Flag Action, Curation Flag

**Curation Action**:
A specific fix directive attached to a Curation Request. Values: `DIFFICULTY_TOO_EASY` (raise difficulty), `DIFFICULTY_TOO_HARD` (lower difficulty), `DELETE` (card is duplicate or worthless), `BACKTICK_REDO` (inline-code formatting is wrong), `NEEDS_CODE_EXAMPLE` (answer needs a code block), `FULL_REDO` (factually wrong or structurally broken). `DIFFICULTY_TOO_EASY` and `DIFFICULTY_TOO_HARD` are mutually exclusive. All other actions can coexist. Presented to the user as: "Too easy," "Too hard," "Duplicate or low quality," "Formatting looks broken," "Needs a code example," "Needs a full rewrite."
_Avoid_: Flag Action, Curation Type, Curation Flag Action

### Activities

**Study Creation**:
The flow a user goes through to start a Study Session. All entry points route through the **Preview Study Session Screen** before the session begins.
- **Single-subcategory**: tap a Subcategory on Category Details (or "Start" in the app bar of Subcategory Details) → Preview Study Session Screen → session begins.
- **Quick Session**: tap "Quick Session" on Category Details → system auto-selects Subcategories and Flashcards (MVP: randomized) → Preview Study Session Screen → session begins.
- **Composite**: tap "Start Composite Session" on Category Details → list enters multi-select → user selects Subcategories → taps Start → Preview Study Session Screen → session begins.
_Avoid_: Session setup, Session wizard

**Browse Search**:
The inline search flow on the Browse screen: a query typed into the search box live-queries Subcategories by name prefix and locally prefix-matches loaded Categories, rendering matches as two sections — Subcategories (labelled **Topic** per the Subcategory UI name) above Categories. Not a separate route; takes no back-stack entry. See [docs/design/category-search.md](docs/design/category-search.md).
_Avoid_: Category search, Topic search (Subcategory is the canonical term outside the UI label; see Subcategory's avoid list)

**Preview Study Session Screen**:
A full-screen preview shown before every Study Session begins. A read-only hero shows session scope (card count, topic count, estimated duration). Below it, a persistent **no-scrim bottom sheet** presents each adjustable session setting as a summary row — Mode (Rated | Fast), Voice answering (Rated only), Voice/TTS (when TTS applies), Length, Filters, Sort — each showing its current value and opening a focused modal edit sheet; plus a "Start session" button and a "Re-randomize" button (multi-topic and Quick sessions only). Only place in the app where Study Mode (and, up front, Voice answering) is chosen for a concrete session — onboarding and the Settings screen only set the persisted default, they don't start a session. Each Preview setting popup (except Filters) also carries a "keep as default" checkbox to update that persisted default from here. See ADR-0030.
_Avoid_: Pre-start Screen (retired name), Pre-session screen, Session config, Mode picker

**Persistent Mastery**:
A cross-session record of Flashcards a User has ever reached a Mastered Terminal State on within a Rated Study Session. Stored in Firestore as `users/{uid}/masteredCards/{cardId}`. A Flashcard is in the Persistent Mastery set iff the User currently holds mastery — mastery is removed (de-mastered) when the card reaches a Failed Terminal State in a subsequent Rated session. Applies to global Flashcards only; Private Flashcards are excluded.
_Avoid_: Permanent mastery, Long-term mastery, Mastered set

**Mastery Defense**:
The mechanic by which previously mastered Flashcards are re-inserted into a Rated Study Session's card pool (up to 10% of the pool). A Mastery Defense card is visually marked with a shield icon during the session. Successfully answering a Mastery Defense card (Correct on any Attempt) retains Persistent Mastery and earns bonus XP. Failing (Failed Terminal State) removes the card from Persistent Mastery (de-mastery) and costs XP. Mastery Defense applies to Rated sessions only.
_Avoid_: Review card, Recall challenge

**XP (Experience Points)**:
Points earned by a User through study activities (card mastery, time studied, session completion, daily goal, streak). Accumulate toward the next Level. XP can decrease when a previously mastered Flashcard is de-mastered (XP loss), but Level never decreases — XP loss is clamped to the current Level's floor.
_Avoid_: Score, Points, Credits

**Level**:
A User's progression milestone derived from total XP accumulated. Early levels are fast to achieve; later levels require significantly more XP (super-linear curve). Level-up triggers a celebration animation. Milestone levels (5, 10, 25, 50, 100) unlock badges. Level is displayed on the Progress screen and in the medium launcher widget.
_Avoid_: Rank, Tier, Grade

**Streak**:
The count of consecutive calendar days on which a User started at least one Study Session (partial or full). Day boundary is midnight in the device's local timezone. A Streak breaks when a full calendar day passes without a session. Best Streak is the historical peak; it never decrements.
_Avoid_: Combo, Daily count

**Daily Goal**:
A User-configured target for minutes studied per calendar day. Default: 20 minutes. Set during onboarding (skippable) and editable inline on the Progress screen. Meeting the Daily Goal awards XP once per calendar day.
_Avoid_: Study target, Quota

**Sessions Completed**:
Count of Study Sessions in which the User reached deck end (last card completed). Partial sessions (exited before deck end) do not count. Tracked as a lifetime aggregate stat on the Progress screen.
_Avoid_: Sessions finished, Sessions done


## Relationships

- A **Category** contains one or more **Subcategories**
- A **Flashcard** belongs to exactly one **Subcategory**
- A **Flashcard** carries one or more **Tags** (all global, all user-facing) and a **Private flag**; the same Tag may appear on Flashcards across different Subcategories
- A **Study Session** draws **Flashcards** from one or more **Subcategories** within a single **Category**
- A **Recent** is a past **Study Session** — single-subcategory if one Subcategory, composite if multiple
- A **Favorite** is a bookmarked **Subcategory**
- An **Attempt** produces exactly one **Rating** *(Rated sessions only)*
- **Voice Answering** is a toggle available only within a **Rated** Study Session; its automatic grade produces a **Rating** the same way a manual tap does
- A **Flashcard** in a **Rated** Study Session has at most 3 **Attempts**
- A **Terminal State** of Mastered means the Flashcard received a Correct **Rating**; Failed means 3 Attempts passed without Correct *(Rated sessions only)*
- A **Flashcard** in **Persistent Mastery** is eligible to appear as a **Mastery Defense** card in future Rated Study Sessions
- A **User** accumulates **XP** through study activity; XP determines **Level**
- A **Streak** belongs to a **User** and increments once per calendar day a Study Session is started
- A **Daily Goal** belongs to a **User** and is compared against today's total studied minutes

