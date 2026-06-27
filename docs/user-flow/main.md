# User Flow — Main

Authentication entry/exit and the navigation shell. Each tab's internal flows are in their own file.

```mermaid
flowchart TD

    %% Legend: (Screen)  [/Action/]  {Decision}  ([Entry/Exit])  {{NYI}}

    %% ── App Launch ────────────────────────────────────────────────
    AppLaunch([App Launch]) --> Splash(SPLASH SCREEN)
    Splash --> AuthCheck{Authenticated?}
    AuthCheck -->|yes| NavRoot
    AuthCheck -->|no| Login(LOGIN SCREEN\nGoogle Sign-In)

    Login -->|sign-in success| Onboarding{{ONBOARDING — NYI\ndaily goal setup · skippable}}
    Onboarding --> NavRoot

    %% ── Navigation Root ───────────────────────────────────────────
    NavRoot(MAIN SCREEN\nbottom nav)
    NavRoot --> HomeTab[🏠 Home — see home.md]
    NavRoot --> StudyTab[📚 Study — see study.md]
    NavRoot --> ProgressTab[📊 Progress — see progress.md]
    NavRoot --> SettingsTab[⚙️ Settings — see settings.md]

    %% ── Cross-tab ─────────────────────────────────────────────────
    HomeTab -.->|"Start your first session" CTA\ntab switch · no stack push| StudyTab
```
