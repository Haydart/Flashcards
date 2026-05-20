# Flashcards

A mobile app for studying curated flashcards organized by topic, with AI-assisted session planning and spaced-repetition-influenced flashcard selection.

## Language

### Entities

**Category**:
A top-level knowledge domain a user can study (e.g. Android, Python).
_Avoid_: Topic, Subject, Domain

**Subcategory**:
A granular area within a Category. A Flashcard belongs to exactly one Subcategory (e.g. Compose, Coroutines, Navigation under Android).
_Avoid_: Subtopic, Topic

**Tag**:
A broader umbrella concept that groups multiple Subcategories (e.g. "UI" spans Compose, XML, Navigation, Notifications). Tags are internal and AI-facing only — they never appear in any user-facing UI.
_Avoid_: Label, Topic, Filter

**Flashcard**:
A question-answer pair belonging to exactly one Subcategory. Part of the global admin-curated pool or a user's private collection.
_Avoid_: Card, Question

**Private Flashcard**:
A Flashcard created by a user. Follows a submission lifecycle: `private → submitted → approved` (approved flashcards may be promoted to the global pool).
_Avoid_: User card, Custom card

**Curriculum**:
The set of Categories a user has actively added to their Learn tab. Drives which Flashcards surface in the Curriculum Algorithm.
_Avoid_: Topics, Interests, Enrolled categories

**Curriculum Algorithm**:
The client-side logic that selects and orders Flashcards for a Study Session based on per-flashcard performance scores and recency weights.
_Avoid_: Recommendation engine, Spaced repetition algorithm (broader term; this is a specific client-side implementation)

**User**:
An authenticated person using the app. Represented in code as `AuthUser` with `uid`, `email`, `displayName`, `photoUrl`.
_Avoid_: Account, Player, Learner

**Study Session**:
A focused learning instance scoped to one Category and a set of selected Subcategories. Flashcards are drawn via the Curriculum Algorithm. Only Flashcards that reached a Terminal State are written to Firestore — queued Flashcards at session end are treated as unseen.
_Avoid_: Quiz, Session alone (ambiguous with auth session)

**In-Progress Session**:
A Study Session that was interrupted before reaching a Terminal State for all Flashcards (app kill, navigation away). Persisted locally to enable resumption.
_Avoid_: Paused session, Saved session

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
The multi-step configuration flow a user goes through before a Study Session begins: intent input (optional, AI-assisted) → subcategory selection → session parameters (MVP: skipped).
_Avoid_: Session setup, Session wizard

**Onboarding**:
The first-login flow that prompts the User to select their initial Curriculum Categories before reaching the Learn tab.
_Avoid_: Setup, Welcome flow, Registration

## Relationships

- A **Category** contains one or more **Subcategories**
- A **Flashcard** belongs to exactly one **Subcategory**
- A **Flashcard** carries one or more **Tags** (internal/AI-facing only)
- A **Study Session** draws **Flashcards** from the selected **Subcategories** within one **Category**
- An **Attempt** produces exactly one **Rating**
- A **Flashcard** in a **Study Session** has at most 3 **Attempts**
- A **Terminal State** of Mastered means the Flashcard received a Correct **Rating**; Failed means 3 Attempts passed without Correct

