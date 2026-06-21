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
- **Rated**: user reveals each answer manually, then self-rates (Failed / Partial / Correct). Flashcards reaching a Terminal State are written to Firestore.
- **Fast**: system TTS reads the question aloud, pauses, reads the answer aloud, then auto-advances. User controls playback via transport controls (pause / play / skip / speed slider). Playback continues with the screen off or app backgrounded. No Ratings, no Attempts, no Terminal States.
_Avoid_: Automatic mode, Passive mode, Browse mode, Voice mode (voice is the delivery mechanism, not the mode name)

**Attempt**:
A single presentation of a Flashcard to the user within a **Rated** Study Session. Each Flashcard has a maximum of 3 Attempts per session. Does not apply to Fast Study Sessions.
_Avoid_: Turn, Round, Try

**Rating**:
The user's self-assessment after viewing an answer in a **Rated** Study Session. Values: **Failed**, **Partial**, **Correct**. A Partial or Failed Rating triggers re-insertion of the Flashcard into the session queue; Correct ends the Flashcard's Attempts. Does not apply to Fast Study Sessions.
_Avoid_: Score, Grade, Answer, Response

**Terminal State**:
A Flashcard's final outcome in a **Rated** Study Session. A Flashcard reaches a Terminal State when it receives a Correct Rating (any Attempt) or exhausts all 3 Attempts without a Correct Rating. Terminal states written to Firestore: **Mastered** (Correct) or **Failed** (not Correct in 3 Attempts). Partial on the 3rd Attempt resolves to Failed. Does not apply to Fast Study Sessions.
_Avoid_: Final state, End state, Result

**Mastered**:
The Terminal State of a Flashcard that received a Correct Rating within a **Rated** Study Session.
_Avoid_: Completed, Passed, Correct (Correct is the Rating that causes Mastered, not a synonym)

**Flag**:
A user-submitted signal that a Flashcard needs admin attention. One Flag per Flashcard per User; mutable (overwritable). Two action values: **Retire** (card should be deleted — too obscure or irrelevant) and **Rework** (card should be edited — imprecise or poorly worded). Flags are stored at `users/{uid}/flaggedCards/{cardId}`. No personal suppression — flagged Flashcards still appear in Study Sessions. Managed via the **Flags Screen**.
_Avoid_: Report, Suggest, Propose, Mark-for-deletion

**Flag Action**:
The intent carried by a Flag. Values: **Retire** (delete the Flashcard from the global pool) or **Rework** (edit the Flashcard for quality). Chosen by the User at flag time; changeable later from the Flags Screen.
_Avoid_: Flag type, Flag reason, Flag status

### Activities

**Study Creation**:
The flow a user goes through to start a Study Session. All entry points route through the **Pre-start Screen** before the session begins.
- **Single-subcategory**: tap a Subcategory on Category Details (or "Start" in the app bar of Subcategory Details) → Pre-start Screen → session begins.
- **Quick Session**: tap "Quick Session" on Category Details → system auto-selects Subcategories and Flashcards (MVP: randomized) → Pre-start Screen → session begins.
- **Composite**: tap "Start Composite Session" on Category Details → list enters multi-select → user selects Subcategories → taps Start → Pre-start Screen → session begins.
_Avoid_: Session setup, Session wizard

**Pre-start Screen**:
A full-screen summary shown before every Study Session begins. Displays session scope (card count, topic count, estimated duration). Below the stats row: a pill-button radio group for **Study Mode** selection (Rated | Fast); tapping a pill shows a short description of that mode beneath the group. Default selection: Rated. Contains a "Start session" button that launches the session with the selected mode. Only place in the app where Study Mode is chosen.
_Avoid_: Pre-session screen, Session config, Mode picker


## Relationships

- A **Category** contains one or more **Subcategories**
- A **Flashcard** belongs to exactly one **Subcategory**
- A **Flashcard** carries one or more **Tags** (all global, all user-facing) and a **Private flag**; the same Tag may appear on Flashcards across different Subcategories
- A **Study Session** draws **Flashcards** from one or more **Subcategories** within a single **Category**
- A **Recent** is a past **Study Session** — single-subcategory if one Subcategory, composite if multiple
- A **Favorite** is a bookmarked **Subcategory**
- An **Attempt** produces exactly one **Rating** *(Rated sessions only)*
- A **Flashcard** in a **Rated** Study Session has at most 3 **Attempts**
- A **Terminal State** of Mastered means the Flashcard received a Correct **Rating**; Failed means 3 Attempts passed without Correct *(Rated sessions only)*

