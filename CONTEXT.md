# Flashcards

A mobile app for studying curated flashcards organized by topic, with spaced-repetition-influenced flashcard selection.

## Language

### Entities

**Category**:
A top-level knowledge domain a user can study (e.g. Android, Python).
_Avoid_: Topic, Subject, Domain

**Subcategory**:
A granular area within a Category. A Flashcard belongs to exactly one Subcategory (e.g. Compose, Coroutines, Navigation under Android). Displayed to users as **Topic** in the UI; Subcategory is the canonical term in code and internal docs.
_Avoid_: Subtopic (the UI label "Topic" is intentional and not a synonym to avoid — it is the presentation-layer name)

**Tag**:
A broader umbrella concept that groups multiple Subcategories (e.g. "UI" spans Compose, XML, Navigation, Notifications). Tags are internal and AI-facing only — they never appear in any user-facing UI.
_Avoid_: Label, Topic, Filter

**Flashcard**:
A question-answer pair belonging to exactly one Subcategory. Part of the global admin-curated pool or a user's private collection.
_Avoid_: Card, Question

**Private Flashcard**:
A Flashcard created by a user. Follows a submission lifecycle: `private → submitted → approved` (approved flashcards may be promoted to the global pool).
_Avoid_: User card, Custom card

**Favorite**:
A Subcategory explicitly bookmarked by a User. Displayed on the Home screen as a carousel of cards showing Subcategory and parent Category names.
_Avoid_: Starred, Saved, Liked

**Recent**:
A past Study Session surfaced on the Home screen as a carousel card. Renders as one of two variants: a **single-topic Recent** (session had one Subcategory — shows Subcategory + Category name, taps into Subcategory Details) or a **composite Recent** (session spanned multiple Subcategories — shows Category name only, taps into Category Details).
_Avoid_: History, Last session

**User**:
An authenticated person using the app. Represented in code as `AuthUser` with `uid`, `email`, `displayName`, `photoUrl`.
_Avoid_: Account, Player, Learner

**Study Session**:
A focused learning instance scoped to one or more Subcategories within a single Category. Flashcards are selected and ordered by per-flashcard performance scores and recency weights. Only Flashcards that reached a Terminal State are written to Firestore — queued Flashcards at session end are treated as unseen. A session with exactly one Subcategory is a **single-topic session**; a session spanning multiple Subcategories is a **composite session**.
_Avoid_: Quiz, Session alone (ambiguous with auth session)

**Attempt**:
A single presentation of a Flashcard to the user within a Study Session. Each Flashcard has a maximum of 3 Attempts per session.
_Avoid_: Turn, Round, Try

**Rating**:
The user's self-assessment after viewing an answer. Values: **Failed**, **Partial**, **Correct**. A Partial or Failed Rating triggers re-insertion of the Flashcard into the session queue; Correct ends the Flashcard's Attempts.
_Avoid_: Score, Grade, Answer, Response

**Terminal State**:
A Flashcard's final outcome in a Study Session. A Flashcard reaches a Terminal State when it receives a Correct Rating (any Attempt) or exhausts all 3 Attempts without a Correct Rating. Terminal states written to Firestore: **Mastered** (Correct) or **Failed** (not Correct in 3 Attempts). Partial on the 3rd Attempt resolves to Failed.
_Avoid_: Final state, End state, Result

**Mastered**:
The Terminal State of a Flashcard that received a Correct Rating within a Study Session.
_Avoid_: Completed, Passed, Correct (Correct is the Rating that causes Mastered, not a synonym)

### Activities

**Study Creation**:
The flow a user goes through to start a Study Session. Three entry points, all originating from the Study screen:
- **Single-topic**: tap a Subcategory on Category Details (or "Start" in the app bar of Subcategory Details) → single-topic session begins.
- **Quick Session**: tap "Quick Session" on Category Details → system auto-selects Subcategories and Flashcards based on performance scores (MVP: randomized) → composite session begins immediately.
- **Composite**: tap "Start Composite Session" on Category Details → screen enters multi-select mode on the Subcategory list → user selects topics → taps start → composite session begins.
_Avoid_: Session setup, Session wizard


## Relationships

- A **Category** contains one or more **Subcategories**
- A **Flashcard** belongs to exactly one **Subcategory**
- A **Flashcard** carries one or more **Tags** (internal/AI-facing only)
- A **Study Session** draws **Flashcards** from one or more **Subcategories** within a single **Category**
- A **Recent** is a past **Study Session** — single-topic if one Subcategory, composite if multiple
- A **Favorite** is a bookmarked **Subcategory**
- An **Attempt** produces exactly one **Rating**
- A **Flashcard** in a **Study Session** has at most 3 **Attempts**
- A **Terminal State** of Mastered means the Flashcard received a Correct **Rating**; Failed means 3 Attempts passed without Correct

