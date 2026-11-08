package app.olhonasnuvens.tv.game

import org.junit.Assert.assertEquals
import org.junit.Test

class FixedStepperTest {
    @Test
    fun theFirstFrameOnlyAnchorsTheClock() {
        val stepper = stepper()
        assertEquals(0, stepper.advanceNanos(1_000_000_000L))
        assertEquals(1, stepper.advanceSeconds(1f / 60f))
    }

    @Test
    fun aLongHitchDoesNotRunAway() {
        val stepper = stepper()
        assertEquals(5, stepper.advanceSeconds(1f))
        assertEquals(1, stepper.advanceSeconds(1f / 60f))
        assertEquals(0, stepper.advanceSeconds(-3f))
    }

    @Test
    fun resetDropsTheAnchorSoBackgroundTimeIsIgnored() {
        val stepper = stepper()
        stepper.advanceNanos(5_000_000_000L)
        stepper.advanceNanos(9_000_000_000L)
        stepper.reset()
        assertEquals(0, stepper.advanceNanos(20_000_000_000L))
    }

    private fun stepper(): FixedStepper = FixedStepper(
        fixedDt = 1f / 60f,
        maxFrameSeconds = 0.25f,
        maxSteps = 5,
    )
}
