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
    Results -->|no match| NoMatch(EMPTY STATE\nNo topics match ...)
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
    TapFastStart --> PreviewStudySession(PREVIEW STUDY SESSION SCREEN)
    TapQuickSession --> PreviewStudySession
    CatAddShortcut --> ShortcutPinDialog[Android system\nshortcut pin dialog]

    TapComposite --> CatMultiselect(CATEGORY DETAILS\nmulti-select active)
    CatMultiselect --> SelectSubcats[/Select ≥1 subcategories/]
    SelectSubcats --> TapStartComposite[/Tap Start/]
    TapStartComposite --> PreviewStudySession

    %% ── Subcategory Details ───────────────────────────────────────
    SubcatDetails(SUBCATEGORY DETAILS SCREEN\nFlashcard list · collapsible items\nApp bar: back · bookmark · overflow)
    SubcatDetails --> TapFilterIcon[/Tap filter icon — bottom toolbar\nbadge dot when non-default/]
    SubcatDetails --> TapSortIcon[/Tap sort icon — bottom toolbar\nbadge dot when non-default/]
    SubcatDetails --> TapStartSession[/Tap Start Session — trailing FAB/]
    SubcatDetails --> TapFAB[/Add(+) icon — Create Private Flashcard/]
    SubcatDetails --> SubcatAddShortcut[/⋮ → Add to home screen/]

    SubcatAddShortcut --> ShortcutPinDialog

    TapFilterIcon --> FilterSheet(FILTER SHEET\nTags — Select All · Unselect All\nchips + Private chip · OR semantics\n+ Difficulty RangeSlider 1–10 · AND with tags\napplies on close · no Apply button)
    FilterSheet -->|dismiss| SubcatDetails

    TapSortIcon --> SortMenu(SORT MENU\nDefault · Easiest first · Hardest first\nreorders list only)
    SortMenu -->|select| SubcatDetails

    TapStartSession -->|filterTagIds = active tags| PreviewStudySession

    TapFAB --> CreateCard(CREATE PRIVATE FLASHCARD SCREEN\nQuestion · Answer · Tags)
    CreateCard -->|Save| SubcatDetails
```

## Session

> Study Mode selection happens on the Preview Study Session Screen; a session routed with Fast mode auto-starts voice playback (ADR-0004).
>
> **Mastery Defense (Rated only):** At Preview Study Session card selection time, up to 10% of the resolved pool is silently filled with previously mastered cards from the same scope. These appear in session with a small shield icon. No user interaction required; transparent to the user.

```mermaid
flowchart TD

    %% Legend: (Screen)  [/Action/]  {Decision}  ([Entry/Exit])

    %% ── Preview Study Session Screen ──────────────────────────────────────────
    PreviewStudySession(PREVIEW STUDY SESSION SCREEN\nScope preview · card count · estimated duration\nStudy Mode: Rated or Fast\nStart Session button)
    PreviewStudySession --> ModeChoice{Study Mode?}
    ModeChoice -->|Rated| RatedSession
    ModeChoice -->|Fast| FastSession

    %% ── Rated Session ─────────────────────────────────────────────
    RatedSession(STUDY SESSION — RATED\nX/N mastered · Attempt X of N\nN = Settings-configured Attempts limit, default 3\nShield icon on mastery defense cards)
    RatedSession --> ShowAnswer[/Tap 'Show Answer' or swipe up/]
    ShowAnswer --> AnswerRevealed(Answer state)
    AnswerRevealed --> Rate[/Rate: Failed · Partial · Correct/]
    Rate --> RateOutcome{Outcome?}

    RateOutcome -->|"Correct (any attempt)"| CardMastered[Mastered · X++\nauto-advance]
    RateOutcome -->|"Partial or Failed\nattempt < N"| ReInsert[Re-inserted · attempt++\nauto-advance]
    RateOutcome -->|"Partial or Failed\nattempt = N"| TerminalFailed[Terminal Failed\nauto-advance]

    CardMastered & ReInsert & TerminalFailed --> QueueCheck{Queue empty?}
    QueueCheck -->|no| RatedSession
    QueueCheck -->|yes| WriteFull[Write to Firestore:\nTerminal States + session record\nisPartial = false]
    WriteFull --> Summary

    %% Extended context (Rated)
    AnswerRevealed -->|extendedContext present| TapMore[/Tap 'See more'/]
    TapMore --> ExtCtxDialog(EXTENDED CONTEXT DIALOG\nexplanation · code · references)
    ExtCtxDialog -->|dismiss| AnswerRevealed

    %% Report a problem (Rated)
    RatedSession --> TapFlag[/Tap flag icon\noutline · top-right of card/]
    TapFlag --> ReportSheet(REPORT A PROBLEM SHEET\n6 toggleable reasons · Cancel · Submit)
    ReportSheet -->|Submit ≥1 reason| CurationUpsert[Curation Actions upserted to Firestore\ncard stays in queue]
    ReportSheet -->|Cancel| RatedSession
    CurationUpsert --> RatedSession

    %% Premature exit (Rated)
    RatedSession --> TapX1[/Tap X — top-left/]
    TapX1 --> ExitConfirm1{Confirm exit?\nProgress will be lost}
    ExitConfirm1 -->|Cancel| RatedSession
    ExitConfirm1 -->|Confirm| WritePartial1[Write to Firestore:\nTerminal States so far + session record\nisPartial = true]
    WritePartial1 --> Summary

    %% ── Fast Session ──────────────────────────────────────────────
    FastSession(STUDY SESSION — FAST\nTTS auto-play · no ratings · no mastery)
    %% Controls available during Fast session — no navigation change:
    %% pause/play · skip-next · skip-previous · speed slider 0.5×–2×
    %% Show Answer: interrupts Q utterance, reads A immediately
    %% Playback survives screen-off/app-background via foreground MediaSession service
    FastSession --> TtsLoop[TTS: Q → 1.5s pause → A → 2.5s pause → auto-advance]
    TtsLoop --> FastQueueCheck{Queue empty?}
    FastQueueCheck -->|no| FastSession
    FastQueueCheck -->|yes| WriteFast[Write to Firestore:\nsession record · isPartial = false\nno card mastery written]
    WriteFast --> Summary

    %% Extended context (Fast — voice-aware)
    FastSession -->|extendedContext present| TapMoreFast[/Tap 'See more'/]
    TapMoreFast --> ExtCtxFast(EXTENDED CONTEXT DIALOG\nVoice pauses silently at between-card silence\nAuto-advances 500ms after dismiss\nNo auto-resume if user had manually paused)
    ExtCtxFast -->|dismiss| FastSession

    %% Report a problem (Fast)
    FastSession --> TapFlagFast[/Tap flag icon/]
    TapFlagFast --> ReportSheetFast(REPORT A PROBLEM SHEET\n6 toggleable reasons · Cancel · Submit)
    ReportSheetFast -->|Submit ≥1 reason| CurationUpsertFast[Curation Actions upserted to Firestore\ncard stays in queue]
    ReportSheetFast -->|Cancel| FastSession
    CurationUpsertFast --> FastSession

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

    StudyAgainAll -->|"same categoryId + subcategoryIds\npopUpTo Main · card re-selection runs fresh"| PreviewStudySession
    StudyAgainFailed -->|"cardIds = failed cards only\nbypasses PreviewStudySession · popUpTo Main"| RatedSession
    GoBack -.->|"popUpTo Main (inclusive=false)\nreturns to whichever tab was active"| NavRoot([Main Screen])
```
