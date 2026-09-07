package com.rossomak.flashcards.core.domain.model

import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.Test

class SessionClockTest {

    private val t0 = Instant.parse("2026-09-06T10:00:00Z")

    @Test
    fun `a fresh clock reports zero elapsed before the first start`() {
        val clock = SessionClock()

        elapsedSeconds(clock, t0) shouldBe 0
    }

    @Test
    fun `a start then a later report measures the span between them`() {
        val clock = startClock(SessionClock(), t0)

        elapsedSeconds(clock, t0.plusSeconds(30)) shouldBe 30
    }

    @Test
    fun `a start-stop-start-stop sequence sums both spans and excludes the gap between them`() {
        var clock = startClock(SessionClock(), t0)
        clock = stopClock(clock, t0.plusSeconds(10))
        clock = startClock(clock, t0.plusSeconds(100)) // 90-second gap must not accrue
        clock = stopClock(clock, t0.plusSeconds(115))

        elapsedSeconds(clock, t0.plusSeconds(500)) shouldBe 25
    }

    @Test
    fun `starting an already-running clock is a no-op, not a double-count`() {
        val clock = startClock(SessionClock(), t0)

        val startedAgain = startClock(clock, t0.plusSeconds(5))

        startedAgain shouldBe clock
    }

    @Test
    fun `stopping an already-stopped clock is a no-op`() {
        val clock = stopClock(startClock(SessionClock(), t0), t0.plusSeconds(10))

        val stoppedAgain = stopClock(clock, t0.plusSeconds(999))

        stoppedAgain shouldBe clock
    }

    @Test
    fun `reporting while running includes the current running span`() {
        val clock = startClock(SessionClock(), t0)

        elapsedSeconds(clock, t0.plusSeconds(7)) shouldBe 7
    }

    @Test
    fun `reporting while stopped excludes any time since the stop`() {
        val clock = stopClock(startClock(SessionClock(), t0), t0.plusSeconds(7))

        elapsedSeconds(clock, t0.plusSeconds(999)) shouldBe 7
    }

    @Test
    fun `elapsed never goes negative even when the caller passes a non-monotonic instant`() {
        val clock = startClock(SessionClock(), t0)

        elapsedSeconds(clock, t0.minusSeconds(60)) shouldBe 0
    }

    @Test
    fun `stopping with a non-monotonic instant accrues nothing rather than going negative`() {
        val clock = startClock(SessionClock(), t0)

        val stopped = stopClock(clock, t0.minusSeconds(60))

        elapsedSeconds(stopped, t0.plusSeconds(1000)) shouldBe 0
    }

    @Test
    fun `seconds rounding is consistent between a running report and a stopped report of the same span`() {
        val runningReport = elapsedSeconds(startClock(SessionClock(), t0), t0.plusSeconds(42))
        val stoppedClock = stopClock(startClock(SessionClock(), t0), t0.plusSeconds(42))
        val stoppedReport = elapsedSeconds(stoppedClock, t0.plusSeconds(9999))

        runningReport shouldBe stoppedReport
    }
}
