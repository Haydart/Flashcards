# User Journey

Complete user journey flowchart covering the auth flow, onboarding, all three main tabs,
study session mechanics, and browse/curriculum interactions.

Implemented screens are marked in the diagram comments. Everything else is planned per SYSTEMDESIGN.md.

```mermaid
flowchart TD
    %% ── Auth (implemented) ────────────────────────────────
    Launch([App Launch]) --> Splash[Splash Screen\nAnimated gradient]
    Splash --> AuthCheck{Authenticated?}
    AuthCheck -->|yes| IsFirst{First login?}
    AuthCheck -->|no| Login[Login Screen\nGoogle Sign-In]
    Login -->|success| IsFirst
    Login -->|error| Login

    %% ── Onboarding ────────────────────────────────────────
    IsFirst -->|yes| Onboarding[Onboarding\nSelect starting categories]
    IsFirst -->|no| NavRoot
    Onboarding -->|continue / skip| NavRoot

    %% ── Main navigation ───────────────────────────────────
    NavRoot[Bottom Nav\nLearn / Browse / Settings]
    NavRoot --> Learn
    NavRoot --> Browse
    NavRoot --> Settings

    %% ── Learn tab ─────────────────────────────────────────
    Learn[Learn Tab] --> HasCats{Has categories?}
    HasCats -->|no| LearnEmpty[Empty state]
    HasCats -->|yes| CatGrid[Category grid\nmastery % per card]
    LearnEmpty -->|FAB| BrowseFlow[Browse to add category]
    CatGrid --> BannerCheck{In-progress session?}
    BannerCheck -->|yes| Banner[In-progress banner]
    BannerCheck -->|no| TapCat[Tap category]
    Banner -->|Continue| Session
    Banner -->|Delete| CatGrid
    TapCat --> SC1[Study Creation\nStep 1: Intent input\nAI-assisted, optional]
    SC1 --> SC2[Study Creation\nStep 2: Subcategory selection]
    SC2 --> Session

    %% ── Study session ─────────────────────────────────────
    Session[Study Session] --> ShowQ[Show flashcard\nquestion]
    ShowQ --> Reveal[Tap to reveal answer]
    Reveal --> Rate{Rate}
    Rate -->|Correct| Mastered[Mastered terminal]
    Rate -->|Partial| Reinsert[Re-insert in queue]
    Rate -->|Failed| Attempts{3rd attempt?}
    Attempts -->|no| Reinsert
    Attempts -->|yes| Failed[Failed terminal]
    Mastered --> QueueDone{Queue empty?}
    Failed --> QueueDone
    Reinsert --> ShowQ
    QueueDone -->|no| ShowQ
    QueueDone -->|yes| Summary[Session Summary]
    Session -->|interrupted| Saved[(Local storage\nin-progress state)]

    %% ── Session summary ───────────────────────────────────
    Summary --> AgainAll[Study Again All]
    Summary --> AgainFailed[Study Again Failed]
    Summary --> BackLearn[Back to Learn]
    AgainAll --> Session
    AgainFailed --> Session
    BackLearn --> Learn

    %% ── Browse tab ────────────────────────────────────────
    Browse[Browse Tab] --> AllCats[All categories grid]
    AllCats --> TapBrowse[Tap category]
    TapBrowse --> CardList[Flashcard list\nsubcategory filters]
    CardList --> AddCurr[Add to curriculum]
    CardList --> RemCurr[Remove from curriculum]
    Browse --> PrivateFAB[FAB: Create private flashcard\nquestion / answer / tags]

    %% ── Settings tab ──────────────────────────────────────
    Settings[Settings Tab] --> Prefs[Session preferences\ndefault card count: 20]
    Settings --> Perms[App permissions]
    Settings --> Voice[Voice settings\nfuture]
```
