# Onboarding Flow

First-run panned gallery: one step per page, swipe (or tap Continue) to advance, explains structure/session modes/mastery, then captures daily goal, session default, and favorites — with a free voice-answering preview along the way as a premium-feature teaser.

Status: design in progress, grilled iteratively. This doc is updated as decisions are made; unresolved items are listed under Open Items.

## Navigation placement

```
Splash → Login → Onboarding (8 screens) → Main
```

- **Login precedes onboarding.** Screen 7 (Favorites) writes to `users/{uid}/favorites`, so a uid must exist first. No guest mode exists yet — see [ADR pending] Auth architecture note below.
- **Onboarding-seen flag**: stored in local DataStore, not Firestore. Device-scoped, not uid-scoped.
  - Consequence (accepted): same device + new account login → onboarding skipped. New device + same account → onboarding shown again.
- **Future guest mode**: build on Firebase Anonymous Auth, not a separate local-only mode. Anonymous sign-in still returns a non-null uid, so it satisfies `SplashViewModel`'s existing `getCurrentAuthUser() != null` check, ADR-0002's sign-out `popUpTo<AuthedGraph>` logic, and Firestore rules (`request.auth != null`, no provider check) without any rework. This keeps Login-before-Onboarding valid even after guest mode ships — a future anonymous user still has a uid by the time onboarding's Favorites screen runs.
  - Side finding (unrelated to onboarding, flagged for separate follow-up): `firestore.rules` only has explicit rules for `users/{uid}/voiceAnswers` and `users/{uid}/curationRequests`. `favorites`, `recentSessions`, `progress`, `privateCards`, `flaggedCards` have no explicit rule → default-deny. Needs its own investigation.

## Screens

### 1. Welcome
- No progress dots — cover screens are conventionally exempt from progress chrome (showing "step 1 of 9" upfront can deter starting).
- Forward-only: swipe right or tap "Get started" advances. Swiping left from Screen 2 back to Screen 1 does nothing (one-way door) — add a rubber-band/bounce feedback on that gesture attempt so it reads as "this is the start," not a dead gesture. Trivial to implement, not blocking.
- Subcopy fixed: *"...how sessions work, how content is organized, and what it takes to master a card."* (generic; avoids naming Category/Topic prematurely — Screen 2 handles that payoff).
- CTA: "Get started"

### 2. Structure (Category / Topic)
- Explains: Category → Topic → Flashcard hierarchy.
- Card eyebrow label "TOPIC" is **correct as designed** — confirmed against `CONTEXT.md`: Subcategory is the canonical code/docs term, but "Topic" is the intentional presentation-layer name (`CONTEXT.md` Subcategory entry, line 14). No change needed.
- CTA: "Continue"

### 3. Flashcard mastery — moved up from Screen 4, now precedes Session modes
- **Reordering**: swapped with Session modes. Two reasons: (1) this screen is pure presentation — no decision, no input, just teaches a concept — and cover-to-decision screens generally read better than decision-to-concept; (2) mastery is the natural setup for the Rated-vs-Fast choice — Rated is defined by rating cards toward mastery, so explaining mastery first makes the next screen's Rated card land immediately instead of introducing an unexplained term.
- Explains Attempts → Rating → Terminal State (Mastered) mechanic. Presentation-only, no user decision — CTA is just "Continue."
- **Attempts limit is becoming Settings-customizable** (default 3, max 5 — see `CONTEXT.md` Attempt/Terminal State entries and `SYSTEMDESIGN.md` Settings Screen, both updated). Copy must not hardcode a specific attempt number (e.g. "the third") since it's no longer a fixed constant.
- **Headline, shortened**: "Cards graduate as you get them right" → **"Get it right, cards graduate."** Same metaphor, same claim, cut from 7 words to 5.
- **Subcopy, shortened and reworked** — three changes from the prior round: (1) cut the "on the first try or the third" phrasing entirely — disliked, and would've hardcoded a number that's now configurable anyway; (2) now names the three Rating values (**Failed, Partial, Correct**) since this is Rating's natural introduction point, freeing Screen 4's Rated card to drop them (see Screen 4 below); (3) dropped the explicit "next, pick how you want to run yours" bridge sentence — it was the third sentence bloating the previous draft, and the screen sequence + Continue button already implies "what's next" without spelling it out. This is a judgment call, reversible if the forward-pointer is missed in review.
  *"Rate each answer Failed, Partial, or Correct. Nail it and the card's mastered — it resurfaces less so you focus on what's shaky."*
- Sample card mock (Attempts dots + "Mastered" badge) unchanged — already terminology-correct.
- **New: XP/Level/progress teaser line**, added below the sample card, small/secondary weight (mirrors Screen 4's hands-free banner — main content stays primary, this doesn't compete visually):
  *"🏆 Every mastered card earns XP — climb Levels and track progress per topic in the Progress tab."*
  - Not a duplicate of Screen 5's XP teaser: Card Mastery is XP's single largest, most frequent source (`docs/design/xp-leveling-system.md`: 100 XP per card, Rated-only) and belongs in its own origin screen; Screen 5's line covers the separate daily-goal XP bonus. Same deliberate-split pattern as Screen 4's Rated-card/hands-free-banner split — each screen names the XP source it's actually responsible for teaching.
  - Terminology checked against `CONTEXT.md`: "XP" and "Level" used correctly (avoid-lists: Score/Points/Credits for XP; Rank/Tier/Grade for Level — none used). "Progress tab" matches the actual bottom-nav label (`docs/design/progress-dashboard.md`: "Home · Study · Progress · Settings").

### 4. Session modes (merged with former defaults screen) — moved down from Screen 3
- Explains Rated vs Fast, **and** captures the user's default Study Mode in the same screen — see rationale under Open Items history (former Screen 5 shrank to a single control once session-size and shuffle were dropped, no longer justified as its own screen).
- Eyebrow: SESSION MODES
- Headline: "Two ways to run a session"
- Subcopy: "Choose your mode each time you start. Pick your default below — change it anytime in Settings."
- Body: reuses the radio-card group component from the Preview Study Session Screen mockup (Rated / Fast cards, Rated pre-selected), but with **taller cards and more explanatory copy per card** — onboarding is a first-time explanation, not a quick pre-session reminder, so it can diverge from the Preview screen's terser text:
  - **Rated**: *"Reveal each answer, then rate yourself — or speak your answer and let the app grade it automatically. Progress is saved."*
  - **Fast**: *"Move through cards at your pace — tap to reveal the answer and advance, or turn on read-aloud to go hands-free."*
  - Content gap this fixed: the original Fast copy never mentioned read-aloud at all, its most distinguishing trait. Also corrected an initial assumption that Fast is TTS-driven by default — it's actually manual-first with read-aloud as an opt-in toggle (see `CONTEXT.md` Study Mode entry, updated to reflect this).
  - Rated card mentions AI-graded voice answering (describes the *behavior* — speak, auto-graded — without naming the feature) but drops both the Attempts/Mastered detail and the explicit Failed/Partial/Correct grade list — both now redundant with Screen 3 (Flashcard mastery), which covers the mechanic *and* names the three grades immediately before this screen. Removing the grade list here keeps the card generic ("rate yourself") and shortens it now that Screen 3 owns that detail.
  - The hands-free banner below supplies the feature *name* ("Voice Answering") that the Rated card's behavior description leaves unnamed — deliberate split, not duplication.
- Below the cards, one shared line, deliberately shrunk (smaller type, single line, no longer restating each mode's mechanic since the cards now own that) so it doesn't compete visually with the primary mode-choice content:
  *"🎙 Both modes can go hands-free — turn on read-aloud in Fast, or Voice Answering in Rated. Great for a walk or commute."*
  - Terminology fix carried over: previously said "Voice mode," which `CONTEXT.md` explicitly bans (Study Mode and Voice Answering entries both list "Voice mode" as _Avoid_ — reads like a third Study Mode when Voice Answering is actually a Rated-only toggle).
- CTA: "Continue" (standard onboarding pill — explicitly NOT the "Start session" gradient button from the Preview Study Session Screen; this screen sets a default preference, it doesn't launch a session).
- **Session size picker and Shuffle order toggle: removed.**
  - Shuffle order was never a real setting — the Flashcard Selection Algorithm (SYSTEMDESIGN.md) does an internal "slight shuffle" on the picked N cards, not user-configurable. Exposing it as an onboarding toggle invented a preference that doesn't exist in the data model.
  - Session size stays at its existing Settings-only default (20 cards), not surfaced during onboarding — reduces screen count, avoids onboarding bloat.

### 5. Daily goal + XP (new screen — did not exist in original 9)
- **Why added**: `docs/design/progress-dashboard.md` and `docs/design/session-stats-data-model.md` both spec Daily Goal as "set during onboarding (skippable)," but no such screen existed in the original mockup. XP/leveling (`docs/design/xp-leveling-system.md`) was also entirely absent from onboarding despite Card Mastery (Screen 3) being the #1 XP source.
- Combined into one screen rather than two — goal-setting is the functional half, XP/streak is a one-line payoff for why the goal matters.
- Position: fifth in the flow, right after Session modes (Screen 4). No longer directly adjacent to Mastery (Screen 3) after the reorder above, but the XP teaser still conceptually traces back to it — Card Mastery is the XP system's top source — so the link survives one screen removed.
- Eyebrow: YOUR GOAL
- Headline: "Set your daily goal"
- Subcopy: "A few minutes a day beats a big session once a week. Change this anytime in Settings."
- Widget: Daily goal stepper/slider, minutes/day, default 20, step 5, range 5–120. Value display: "20 min / day".
- XP teaser line below widget: "Hit your goal → earn XP, build your streak 🔥"
- CTA: "Continue"
- Skippable (top-right "Skip"): goal defaults to 20 min if skipped.

### 6. Voice input & privacy (Voice Answering) — formerly Screen 7
- Reworked per premium-framing decision below (Rated-only, AI-grading is the paid part; capture/on-device privacy transform is free and previewable by anyone).
- **Premium framing added.** `docs/adr/0024` / `0029` confirm Voice Answering has real, live server-side entitlement enforcement in prod (`users/{uid}/entitlement/premium`) — this is not an NYI feature like reminder notifications, so it stays in onboarding rather than being cut. But enforcement today is reactive-only (a free user can record → upload → get rejected after the fact); no proactive client paywall exists yet. Onboarding must not imply the whole feature is free.
- Split confirmed by user: **voice capture, VAD, and on-device obfuscation are free for everyone** (this is what "Test your voice" already demos — no grading happens in that preview). **Only the AI-grading step is premium.** The privacy-transform demo stays fully interactive and unrestricted, and doubles as a **trailer for the premium feature** — the demo is real and free, the payoff (AI grading) is what's gated.
- Subcopy update:
  *"Optional — use your mic to answer hands-free. Capture and privacy transform run on-device, always free."*
- New line/badge added below subcopy (or near the card), scoped specifically to grading, not the whole screen:
  *"⭐ Premium: automatic AI grading of your spoken answer."*
- "Private voice" card, its description, and the "Test your voice" button/interaction: unchanged — still a free, fully working preview of capture + on-device obfuscation only.

### 7. Favorites — formerly Screen 8
- Headline fixed: "Star a few topics" → **"Favorite a few topics"**.
  - Terminology fix: `CONTEXT.md`'s Favorite entity explicitly bans "Starred" as a synonym (line 43). The screen's own subcopy already correctly said "Favorites get pinned to your home screen" — headline contradicted it.
- Subcopy unchanged.

### 8. All set / Start studying — formerly Screen 9
- Recap badges rework: must reflect the actual decisions the user made during onboarding, in the order they made them — not a mix of real decisions and silent defaults.
  - **Before**: "20 cards/session" (never user-set — session size stays a silent Settings-only default, per Screen 4's note) + "Rated by default" + "2 favorites". Recap showed an untouched default while omitting a setting the user had just actively configured.
  - **After**: three badges, one per real decision, ordered by when the user made it — **Study mode** (Screen 4: "Rated by default" / "Fast by default") → **Daily goal** (Screen 5: e.g. "20 min/day") → **Favorites** (Screen 7: "N favorites"). Session-size badge removed entirely.
- Headline/subcopy/CTA unchanged ("You're all set, {name}!" / "Start studying").

## Open items (pending further grilling)

- User levels: resolved as informational-only (Screen 5 XP teaser), not a separate self-reported skill-level screen.

## Decided against (for now)

- **Voice playback (TTS) settings screen — cut entirely.** Formerly Screen 6: a "Default voice" picker + "Default speed" slider, functionally identical to the `VoiceSettingsDialog` already in Settings. Confirmed real/implemented (not a fake-feature issue like Shuffle-order), but picking a specific TTS voice before the user has ever heard the app talk is a premature decision, and it contradicted Screen 4's own precedent of keeping granular config (session size, shuffle) out of onboarding. Not crucial to convey at this point in the flow — cut, no replacement screen. Voice playback itself is still taught implicitly via Screen 4's Fast-mode card and hands-free banner; fine-tuning the voice stays a Settings-only action.
- **Reminder notifications**: no onboarding screen added. Feature is NYI — asking for a notification permission tied to a feature that does nothing yet is permission fatigue for zero payoff, and revoking + re-asking later is worse UX than asking once when the feature is real. **Revisit this decision as soon as reminder notifications land in the app in any shape** — add a permission + preferred-time screen then, likely positioned near Screen 5 (Daily goal), since "we'll remind you before your streak breaks" is the natural framing.
