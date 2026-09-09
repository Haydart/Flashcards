package com.rossomak.flashcards.core.domain.model

import java.time.Instant

/**
 * The one piece of Study Session termination Rated and Fast actually share — ADR-0045 leaves them
 * no common base class, so this is where it lives instead of on either ViewModel. Each mode builds
 * its own [result] — Rated from its state machine via [sealRatedCardResults], Fast from its own
 * seen-tracking — with any placeholder `durationSeconds`, since the clock is the only
 * thing this function actually needs to know: it stops [clock] at [at] and overwrites
 * [SessionResult.durationSeconds] with the real elapsed seconds. This is where "the clock stops at
 * termination" actually happens, once, for both modes.
 */
fun sealSessionResult(result: SessionResult, clock: SessionClock, at: Instant): SessionResult {
    val stoppedClock = stopClock(clock, at)
    val durationSeconds = elapsedSeconds(stoppedClock, at)
    return when (result) {
        is SessionResult.Rated -> result.copy(durationSeconds = durationSeconds)
        is SessionResult.Fast -> result.copy(durationSeconds = durationSeconds)
    }
}
