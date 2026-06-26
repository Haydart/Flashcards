# Mastery mechanics are Rated-only; Fast sessions are mastery-agnostic

Persistent Mastery, Mastery Defense, XP gain from card mastery, and XP loss from de-mastery apply exclusively to Rated Study Sessions. Fast Study Sessions are entirely excluded — mastered cards are not visually distinguished in Fast mode, no mastery state changes, no card-mastery XP.

Fast mode has no Rating step — TTS reads the question, pauses, reads the answer, then auto-advances. There is no Correct/Failed outcome per card. Applying mastery mechanics to Fast sessions would require either: (a) automatic inference of mastery from listening (not meaningful), or (b) a mid-session tap to self-rate while TTS is running (breaks the passive listening model). Both options degrade the Fast mode UX.

Showing mastered cards as visually distinct in Fast mode without an interaction model was also rejected — the shield icon would appear with no actionable meaning, confusing users.

Fast sessions still earn XP for time studied, session completion, daily goal, and streak — so they are not XP-dead, just lighter on card-specific rewards. This differential naturally incentivises Rated sessions for users who care about leveling, without blocking Fast sessions as a study mode.

Consequence: a user who studies exclusively in Fast mode never gains or loses Persistent Mastery. Their XP growth is slower but uninterrupted.
