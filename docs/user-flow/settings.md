# User Flow — Settings

Settings preferences. Screen scope locked; most items are NYI in implementation (see SYSTEMDESIGN.md's Settings Screen section for the authoritative list). Flag/report management has no screen — see `study.md`'s in-session "Report a problem" flow instead.

```mermaid
flowchart TD

    %% Legend: (Screen)  [/Action/]  {Decision}  ([Entry/Exit])  {{NYI}}

    Settings(SETTINGS SCREEN)

    %% ── Study Sessions ───────────────────────────────────────────
    Settings --> SessionCount{{Session flashcard count\ndefault 20\nNYI}}
    Settings --> Attempts{{Attempts per card\ndefault 3, max 5\nNYI}}
    Settings --> ModeDefault{{Default study mode\nRated | Fast\nmirrors Preview screen's\n"keep as default" checkbox\nNYI}}
    Settings --> SortDefault{{Default sort order\nDefault | Easiest | Hardest\nmirrors Preview screen's\n"keep as default" checkbox\nNYI}}

    %% ── Voice ─────────────────────────────────────────────────────
    Settings --> VoiceSettings[/Tap Voice settings/]
    VoiceSettings --> VoiceDialog(VOICE SETTINGS DIALOG\nvoice selection + playback speed\nimplemented)
    VoiceDialog -->|dismiss| Settings

    Settings --> VoiceAnsweringDefault{{Voice answering default — Rated\nOn | Off · premium-gated, live\nmirrors Preview screen's\n"keep as default" checkbox\nNYI}}
    Settings --> ReadAloudDefault{{Read-aloud/auto-play default — Fast\nmirrors Preview screen's\n"keep as default" checkbox\nNYI}}

    %% ── Notifications ────────────────────────────────────────────
    Settings --> DailyReminder{{Daily reminder toggle\nrow exists; FCM+WorkManager backend NYI}}

    %% ── Permissions ──────────────────────────────────────────────
    Settings --> PermNotif{{Notifications permission\nstatus + link to system settings\nNYI}}
    Settings --> PermMic{{Microphone permission\nstatus + link to system settings\nNYI}}

    %% ── Daily Goal ───────────────────────────────────────────────
    Settings --> DailyGoal{{Daily goal — minutes/day, default 20\nalso editable inline on Progress screen\nsame persisted value\nNYI}}

    %% ── Sign-out ──────────────────────────────────────────────────
    Settings --> TapSignOut[/Tap Sign Out/]
    TapSignOut --> SigningOut[Signing out...\nloading state]
    SigningOut --> ClearStack[popUpTo AuthedGraph\ninclusive = true]
    ClearStack --> Login([LOGIN SCREEN\nsee main.md])
```
