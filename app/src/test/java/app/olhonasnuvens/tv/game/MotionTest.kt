package app.olhonasnuvens.tv.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test
    fun theBlinkIsAShortLidCloseNotAFlicker() {
        assertTrue(Motion.BLINK_CYCLE_SECONDS >= 2.5f)
        assertTrue(Motion.BLINK_DURATION_SECONDS in 0.12f..0.35f)
        assertEquals(1f, Motion.eyelidOpen(0f), 0.0001f)
        assertEquals(1f, Motion.eyelidOpen(0.4f), 0.0001f)
        val mid = Motion.BLINK_CYCLE_SECONDS - Motion.BLINK_DURATION_SECONDS / 2f
        assertEquals(0f, Motion.eyelidOpen(mid), 0.02f)
        val samples = 3600
        var mostlyClosed = 0
        for (index in 0 until samples) {
            val open = Motion.eyelidOpen(index * 0.01f)
            assertTrue(open in 0f..1f)
            if (open < 0.2f) mostlyClosed++
        }
        assertTrue(mostlyClosed < samples * 0.08f)
        assertEquals(Motion.eyelidOpen(1.25f), Motion.eyelidOpen(1.25f), 0f)
    }
}
