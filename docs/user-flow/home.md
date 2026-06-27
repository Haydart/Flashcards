# User Flow — Home

Browsing recent sessions and bookmarked topics.

`CategoryDetails` and `SubcategoryDetails` are full-screen shared screens (no bottom nav). From the Home tab they use `HomeCategoryDetails` / `HomeSubcategoryDetails` route types (ADR-0003). Their internal flows are documented in [study.md](study.md).

```mermaid
flowchart TD

    %% Legend: (Screen)  [/Action/]  {Decision}  ([Entry/Exit])

    Home(HOME SCREEN\nGreeting · Recents carousel · Favorites carousel)

    %% ── Empty state ───────────────────────────────────────────────
    Home -->|both carousels empty| EmptyCTA[/Tap 'Start your first session'/]
    EmptyCTA -.->|tab switch · no stack push| StudyTab[📚 Study tab]

    %% ── Recents ───────────────────────────────────────────────────
    Home --> TapRecent[/Tap Recent session card/]
    TapRecent --> RecentType{Session scope?}
    RecentType -->|Single-subcategory\nshows Subcategory + Category name| SubcatDetails(SUBCATEGORY DETAILS\nsee study.md)
    RecentType -->|Composite\nshows Category name only| CatDetails(CATEGORY DETAILS\nsee study.md)

    %% ── Favorites ─────────────────────────────────────────────────
    Home --> TapFavorite[/Tap Favorite card\nshows Subcategory + Category name/]
    TapFavorite --> SubcatDetails
```
