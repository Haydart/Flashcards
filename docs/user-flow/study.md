# User Flow — Study

Topic browsing, session entry, studying, and session summary. Launcher shortcut deep-links (pinned to device home screen) bypass the Study tab and enter directly at Category Details or Subcategory Details.

## Browse & entry

```mermaid
flowchart TD

    %% Legend: (Screen)  [/Action/]  {Decision}  ([Entry/Exit])

    StudyEntry([Study tab]) --> StudyScreen
    ShortcutEntry([Launcher shortcut\ndeep-link]) --> CatDetails
    ShortcutEntry --> SubcatDetails

    %% ── Study Screen ──────────────────────────────────────────────
    StudyScreen(STUDY SCREEN\nCategory list)
    StudyScreen --> TapCategory[/Tap Category card/]
    StudyScreen --> TypeSearch[/Search bar — min 2 chars/]

    TypeSearch --> Results(SEARCH RESULTS\nTopics section · Categories section)
    Results -->|no match| NoMatch(EMPTY STATE\n"No topics match ...")
    Results --> TapTopicRow[/Tap Topic row\nsingle-subcategory intent/]
    Results --> TapCatInResults[/Tap Category card/]
    TapTopicRow --> SubcatDetails
    TapCatInResults --> CatDetails
    TapCategory --> CatDetails

    %% ── Category Details ──────────────────────────────────────────
    CatDetails(CATEGORY DETAILS SCREEN\nSubcategory list)
    CatDetails --> TapSubcatRow[/Tap Subcategory row/]
    CatDetails --> TapFastStart[/Fast-start on row\nsingle subcat · skips Subcategory Details/]
    CatDetails --> TapQuickSession[/Quick Session\nauto-selects subcategories/]
    CatDetails --> TapComposite[/Start Composite Session\nlist → multi-select mode/]
    CatDetails --> CatAddShortcut[/⋮ → Add to home screen/]

    TapSubcatRow --> SubcatDetails
    TapFastStart --> PreStart(PRE-START SCREEN)
    TapQuickSession --> PreStart
    CatAddShortcut --> ShortcutPinDialog[Android system\nshortcut pin dialog]

    TapComposite --> CatMultiselect(CATEGORY DETAILS\nmulti-select active)
    CatMultiselect --> SelectSubcats[/Select ≥1 subcategories/]
    SelectSubcats --> TapStartComposite[/Tap Start/]
    TapStartComposite --> PreStart

    %% ── Subcategory Details ───────────────────────────────────────
    SubcatDetails(SUBCATEGORY DETAILS SCREEN\nFlashcard list · collapsible items)
    SubcatDetails --> TapFilterIcon[/Tap filter icon\ndot badge when ≥1 tag active/]
    SubcatDetails --> TapStartSession[/Tap Start Session — app bar/]
    SubcatDetails --> TapFAB[/FAB — Create Private Flashcard/]
    SubcatDetails --> TapMultiselectToggle[/Multiselect toggle — bottom toolbar/]
    SubcatDetails --> SubcatAddShortcut[/⋮ → Add to home screen/]

    SubcatAddShortcut --> ShortcutPinDialog

    TapFilterIcon --> TagFilterDialog(TAG FILTER DIALOG\nSelect All · Unselect All\nTag chips + Private chip\nOR semantics · applies on close\nno Apply button)
    TagFilterDialog -->|dismiss| SubcatDetails

    TapStartSession -->|filterTagIds = active tags| PreStart

    TapFAB --> CreateCard(CREATE PRIVATE FLASHCARD SCREEN\nQuestion · Answer · Tags)
    CreateCard -->|Save| SubcatDetails

    TapMultiselectToggle --> CardMultiselect(SUBCATEGORY DETAILS\nmultiselect active)
    CardMultiselect --> SelectCards[/Select ≥1 flashcards/]
    SelectCards --> BulkRetire[/Tap Retire/]
    SelectCards --> BulkRework[/Tap Rework/]
    BulkRetire -->|Flag upserted: RETIRE| SubcatDetails
    BulkRework -->|Flag upserted: REWORK| SubcatDetails
```

## Session

> **Current interim:** Study Mode selection lives as a toggle inside the `StudySession` `TopAppBar`, not on Pre-start Screen. This is temporary — once Pre-start Screen is built, mode selection moves there and the toggle is removed (ADR-0004). The diagram below shows the **target state**.

> **Mastery Defense (Rated only):** At Pre-start card selection time, up to 10% of the resolved pool is silently filled with previously mastered cards from the same scope. These appear in session with a small shield icon. No user interaction required; transparent to the user.

```mermaid
flowchart TD

    %% Legend: (Screen)  [/Action/]  {Decision}  ([Entry/Exit])

    %% ── Pre-start Screen ──────────────────────────────────────────
    PreStart(PRE-START SCREEN\nScope summary · card count · estimated duration\nStudy Mode: Rated ◉ | Fast ○\nStart Session button)
    PreStart --> ModeChoice{Study Mode?}
    ModeChoice -->|Rated| RatedSession
    ModeChoice -->|Fast| FastSession

    %% ── Rated Session ─────────────────────────────────────────────
    RatedSession(STUDY SESSION — RATED\nX/N mastered · Attempt X of 3\nShield icon on mastery defense cards)
    RatedSession --> ShowAnswer[/Tap 'Show Answer' or swipe up/]
    ShowAnswer --> AnswerRevealed(Answer state)
    AnswerRevealed --> Rate[/Rate: Failed · Partial · Correct/]
    Rate --> RateOutcome{Outcome?}

    RateOutcome -->|"Correct (any attempt)"| CardMastered[Mastered · X++\nauto-advance]
    RateOutcome -->|"Partial or Failed\nattempt < 3"| ReInsert[Re-inserted · attempt++\nauto-advance]
    RateOutcome -->|"Partial or Failed\nattempt = 3"| TerminalFailed[Terminal Failed\nauto-advance]

    CardMastered & ReInsert & TerminalFailed --> QueueCheck{Queue empty?}
    QueueCheck -->|no| RatedSession
    QueueCheck -->|yes| WriteFull[Write to Firestore:\nTerminal States + session record\nisPartial = false]
    WriteFull --> Summary

    %% Extended context (Rated)
    AnswerRevealed -->|extendedContext present| TapMore[/Tap 'See more'/]
    TapMore --> ExtCtxDialog(EXTENDED CONTEXT DIALOG\nexplanation · code · references)
    ExtCtxDialog -->|dismiss| AnswerRevealed

    %% Flag dialog (Rated)
    RatedSession --> TapFlag[/Tap flag icon\noutline · top-right of card/]
    TapFlag --> FlagDialog(FLAG DIALOG\nRetire · Rework · Cancel)
    FlagDialog -->|Retire or Rework| FlagUpsert[Flag upserted in Firestore\ncard stays in queue]
    FlagDialog -->|Cancel| RatedSession
    FlagUpsert --> RatedSession

    %% Premature exit (Rated)
    RatedSession --> TapX1[/Tap X — top-left/]
    TapX1 --> ExitConfirm1{Confirm exit?\nProgress will be lost}
    ExitConfirm1 -->|Cancel| RatedSession
    ExitConfirm1 -->|Confirm| WritePartial1[Write to Firestore:\nTerminal States so far + session record\nisPartial = true]
    WritePartial1 --> Summary

    %% ── Fast Session ──────────────────────────────────────────────
    FastSession(STUDY SESSION — FAST\nTTS auto-play · no ratings · no mastery)
    FastSession --> TtsLoop["TTS: reads Q → 1.5s pause → reads A → 2.5s pause → auto-advance
    Controls (no navigation change): pause/play · skip-next · skip-previous
    Speed slider 0.5×–2× · 'Show Answer' interrupts Q and reads A immediately
    Playback survives screen-off and app backgrounded via foreground MediaSession service"]
    TtsLoop --> FastQueueCheck{Queue empty?}
    FastQueueCheck -->|no| FastSession
    FastQueueCheck -->|yes| WriteFast[Write to Firestore:\nsession record · isPartial = false\nno card mastery written]
    WriteFast --> Summary

    %% Extended context (Fast — voice-aware)
    FastSession -->|extendedContext present| TapMoreFast[/Tap 'See more'/]
    TapMoreFast --> ExtCtxFast(EXTENDED CONTEXT DIALOG\nVoice pauses silently at between-card silence\nAuto-advances 500ms after dismiss\nNo auto-resume if user had manually paused)
    ExtCtxFast -->|dismiss| FastSession

    %% Flag dialog (Fast)
    FastSession --> TapFlagFast[/Tap flag icon/]
    TapFlagFast --> FlagDialogFast(FLAG DIALOG\nRetire · Rework · Cancel)
    FlagDialogFast -->|Retire or Rework| FlagUpsertFast[Flag upserted in Firestore\ncard stays in queue]
    FlagDialogFast -->|Cancel| FastSession
    FlagUpsertFast --> FastSession

    %% Premature exit (Fast)
    FastSession --> TapX2[/Tap X — top-left/]
    TapX2 --> ExitConfirm2{Confirm exit?}
    ExitConfirm2 -->|Cancel| FastSession
    ExitConfirm2 -->|Confirm| WritePartial2[Write to Firestore:\nsession record · isPartial = true]
    WritePartial2 --> Summary

    %% ── Session Summary ───────────────────────────────────────────
    Summary(SESSION SUMMARY SCREEN\nXP breakdown — animated line by line\nLevel-up celebration if applicable\n'Session Completed' +500 XP omitted on partial sessions)

    Summary --> StudyAgainAll[/Study Again — All/]
    Summary --> StudyAgainFailed[/Study Again — Failed\nshown only if ≥1 Terminal Failed\nRated sessions only/]
    Summary --> GoBack[/Tap 'Back to Home' · or system back/]

    StudyAgainAll -->|"same categoryId + subcategoryIds\npopUpTo Main · card re-selection runs fresh"| PreStart
    StudyAgainFailed -->|"cardIds = failed cards only\nbypasses PreStart · popUpTo Main"| RatedSession
    GoBack -.->|"popUpTo Main (inclusive=false)\nreturns to whichever tab was active"| NavRoot([Main Screen])
```
