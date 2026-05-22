# User Journey

Complete user journey flowchart covering auth, all three main tabs, session mechanics, and
Subcategory Details. Everything except the auth flow is planned per SYSTEMDESIGN.md.

```mermaid
flowchart TD

    %% ── Auth ────────────────────────────────────────────────
    Launch([App Launch]) --> Splash(Splash Screen)
    Splash --> AuthCheck{Authenticated?}
    AuthCheck -->|no| Login(Login Screen\nGoogle Sign-In)
    Login -->|success| NavRoot
    AuthCheck -->|yes| NavRoot

    %% ── Navigation root ─────────────────────────────────────
    NavRoot(Bottom Nav\nHome · Study · Settings)
    NavRoot --> Settings
    NavRoot --> Home
    NavRoot --> Study

    %% ── Settings ─────────────────────────────────────────────
    Settings(Settings Screen)
    Settings --> Prefs[/Session preferences\nFlashcard count default 20/]
    Settings --> Perms[/App permissions/]
    Settings --> Voice[/Voice settings — deferred/]

    %% ── Home ─────────────────────────────────────────────────
    Home(Home Screen\nGreeting + carousels)
    Home --> BothEmpty{Both carousels\nempty?}
    BothEmpty -->|yes| EmptyHome(Empty state\nStart your first session CTA)
    EmptyHome -.->|tap CTA| Study
    BothEmpty -->|no| Carousels[Recents · Favorites carousels]
    Carousels --> TapRecent[/Tap Recent card/]
    Carousels --> TapFavorite[/Tap Favorite card/]
    TapRecent --> RecentType{Single-topic\nRecent?}
    RecentType -->|yes| SubcatDetails
    RecentType -->|no| CatDetails
    TapFavorite --> SubcatDetails

    %% ── Study ───────────────────────────────────────────────
    Study(Study Screen\nSearch bar + Category list)
    Study --> TapCategory[/Tap Category card/]
    TapCategory --> CatDetails

    %% ── Category Details ─────────────────────────────────────
    CatDetails(Category Details\nTopic list)
    CatDetails --> QuickSession[/Quick Session\nauto-selects Topics + Flashcards/]
    CatDetails --> CompositeFlow[/Start Composite Session\nlist enters multi-select/]
    CatDetails --> FastStart[/Fast-start on Topic row\nsingle-topic immediately/]
    CatDetails --> TapTopic[/Tap Topic row/]
    CompositeFlow --> SelectTopics[/Select ≥1 Topics/]
    SelectTopics --> StartBtn[/Tap Start/]
    TapTopic --> SubcatDetails

    %% ── Subcategory Details ─────────────────────────────────
    SubcatDetails(Subcategory Details\nFlashcard list)
    SubcatDetails --> StartSession[/Start Session\napp bar button/]
    SubcatDetails --> CreatePrivate[/FAB: Create Private Flashcard\nQuestion · Answer · Tags/]

    %% ── Session entry ───────────────────────────────────────
    QuickSession --> Session
    FastStart --> Session
    StartBtn --> Session
    StartSession --> Session

    %% ── Study Session ───────────────────────────────────────
    Session(Study Session\nX/N mastered progress)
    Session -->|Finish Session| Summary
    Session --> ShowQ[Show Flashcard question]
    ShowQ --> Reveal[/Swipe or tap Show Answer/]
    Reveal --> Rate{Rate}
    Rate -->|Correct| Mastered([Mastered — terminal])
    Rate -->|Partial or Failed| AttemptCheck{3rd attempt?}
    AttemptCheck -->|no| Reinsert[Re-insert · Attempt +1]
    AttemptCheck -->|yes| Failed([Failed — terminal])
    Reinsert --> ShowQ
    Mastered --> QueueCheck{Queue empty?}
    Failed --> QueueCheck
    QueueCheck -->|no| ShowQ
    QueueCheck -->|yes| Summary

    %% ── Session Summary ──────────────────────────────────────
    Summary(Session Summary)
    Summary --> AgainAll[/Study Again — All/]
    Summary --> AgainFailed[/Study Again — Failed\nshown only if ≥1 Failed/]
    Summary --> BackHome[/Back to Home/]
    AgainAll -.-> Session
    AgainFailed -.-> Session
    BackHome -.-> Home
```
