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
    CatDetails --> TapCustom[/Start Custom Session\nlist → multi-select mode/]
    CatDetails --> CatAddShortcut[/⋮ → Add to home screen/]

    TapSubcatRow --> SubcatDetails
    TapFastStart --> PreviewStudySession(PREVIEW STUDY SESSION SCREEN)
    TapQuickSession --> PreviewStudySession
    CatAddShortcut --> ShortcutPinDialog[Android system\nshortcut pin dialog]

    TapCustom --> CatMultiselect(CATEGORY DETAILS\nmulti-select active)
    CatMultiselect --> SelectSubcats[/Select ≥1 subcategories/]
    SelectSubcats --> TapStartCustom[/Tap Start/]
    TapStartCustom --> PreviewStudySession

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

> Study Mode selection happens on the Preview Study Session Screen; Fast mode is manual tap-to-reveal/advance by default, with read-aloud as an opt-in toggle (Voice row) that auto-starts voice playback (ADR-0004).
>
> Fast and Rated are **separate screens and routes** (ADR-0045) — the Preview screen picks one, and a session can never switch.
>
> **Mastery Defense (Rated only) — NYI:** design only (see [docs/design/persistent-card-mastery.md](../design/persistent-card-mastery.md)). Planned: mastered cards are never excluded from the pool, and at Preview Study Session card selection time a floor of 10% of the configured Length is topped up with mastered cards when a natural draw falls short. Every mastered card in the session is a defence card, shown with a small shield icon, no user interaction required. The session is always exactly its configured Length.
>
> **Nothing is written to Firestore during a session.** Both modes hand a result to the Session Summary screen, which computes XP and commits everything in one atomic batch (ADR-0014).

```mermaid
flowchart TD

    %% Legend: (Screen)  [/Action/]  {Decision}  ([Entry/Exit])  {{NYI}}

    %% ── Preview Study Session Screen ──────────────────────────────────────────
    PreviewStudySession(PREVIEW STUDY SESSION SCREEN\nScope preview · card count · estimated duration\nStudy Mode: Rated or Fast\nStart Session button)
    PreviewStudySession --> ModeChoice{Study Mode?}
    ModeChoice -->|Rated| RatedSession
    ModeChoice -->|Fast| FastSession

    %% ── Rated Session ─────────────────────────────────────────────
    RatedSession(STUDY SESSION — RATED\nX/N mastered · X counts distinct cards)
    RatedSession --> ShowAnswer[/Tap 'Show Answer' or swipe up/]
    ShowAnswer --> AnswerRevealed(Answer state)
    AnswerRevealed --> Rate[/Rate: Failed · Partial · Correct/]
    Rate --> RecordAttempt[Attempt consumed · card marked Studied\nbest-rating-so-far updated]
    RecordAttempt --> Resolve{Correct, attempts exhausted,\nor Partial-ends-card?}
    Resolve -->|yes| Terminal[Terminal State = best rating achieved\nMastered · Partial · Failed\nheld in the session ledger]
    Resolve -->|no| Requeue[Re-insert at currentIndex + random gap\nFailed 2-4 · Partial 5-9 · seeded]
    Requeue --> RatedSession
    Terminal -.-> AttemptsNYI{{Attempt X of N label\nMastery Defense shield icon on defended cards\nNYI}}

    Terminal --> QueueCheck{Queue empty?}
    QueueCheck -->|no| RatedSession
    QueueCheck -->|yes| HandOffFull[Seal ledger · stamp duration\nisPartial = false\nno Firestore write yet]
    HandOffFull --> Summary

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
    ExitConfirm1 -->|Confirm| HandOffPartial1[Seal ledger with outcomes so far\nisPartial = true\nqueued cards never recorded]
    HandOffPartial1 --> Summary

    %% ── Fast Session ──────────────────────────────────────────────
    FastSession(STUDY SESSION — FAST\nmanual tap-to-reveal/advance by default\nread-aloud opt-in · no ratings · no mastery)
    FastSession --> ManualAdvance[/Tap to reveal answer\ntap to advance/]
    ManualAdvance --> MarkSeen[Answer shown — card marked Studied\nskipping past a question marks nothing]
    MarkSeen --> FastQueueCheck{Queue empty?}
    %% Controls available when read-aloud is on — no navigation change:
    %% pause/play · skip-next · skip-previous · speed slider 0.5×–2×
    %% Show Answer: interrupts Q utterance, reads A immediately
    %% Playback survives screen-off/app-background via foreground MediaSession service
    FastSession -->|read-aloud on| TtsLoop[TTS: Q → 1.5s pause → A → 2.5s pause → auto-advance]
    TtsLoop --> MarkSeen
    FastQueueCheck -->|no| FastSession
    FastQueueCheck -->|yes| HandOffFast[Seal ledger · stamp duration\nisPartial = false\nStudied cards only, no mastery]
    HandOffFast --> Summary

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
    ExitConfirm2 -->|Confirm| HandOffPartial2[Seal ledger with cards seen so far\nisPartial = true]
    HandOffPartial2 --> Summary

    %% ── Session Summary ───────────────────────────────────────────
    Summary(SESSION SUMMARY SCREEN\ntakes sessionId · result via retained holder\nXP breakdown — animated line by line\nLevel-up celebration if applicable\n'Session Completed' +500 XP omitted on partial sessions)
    Summary --> Commit[ONE atomic batch — 4 writes:\nsession doc with embedded outcomes map\npacked progress doc per subcategory\nstate/progressSummary increments\nstate/progression xp/level/streak]

    Commit --> StudyAgainAll[/Study Again — All/]
    Commit --> StudyAgainFailed[/Study Again — Failed\nshown only if ≥1 Terminal Failed\nRated sessions only/]
    Commit --> GoBack[/Tap 'Back to Home' · or system back/]

    StudyAgainAll -->|"same categoryId + subcategoryIds\npopUpTo Main · card re-selection runs fresh"| PreviewStudySession
    StudyAgainFailed -->|"cardIds = failed cards only\nbypasses PreviewStudySession · popUpTo Main"| RatedSession
    GoBack -.->|"popUpTo Main (inclusive=false)\nreturns to whichever tab was active"| NavRoot([Main Screen])
```
