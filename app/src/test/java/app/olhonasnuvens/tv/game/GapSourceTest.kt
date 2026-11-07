package app.olhonasnuvens.tv.game

import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

class GapSourceTest {
    @Test
    fun randomCentersKeepTheGapOnScreen() {
        val tuning = Tuning()
        val source = RandomGapSource(Random(7))
        val gap = tuning.normal.baseGap
        val half = gap / 2f
        repeat(40) {
            val center = source.next(gap, tuning)
            assertTrue(center - half >= 89.5f)
            assertTrue(center + half <= tuning.groundY - 89.5f)
        }
    }
}
