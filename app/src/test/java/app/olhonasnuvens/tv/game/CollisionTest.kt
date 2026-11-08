package app.olhonasnuvens.tv.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionTest {
    @Test
    fun circleTouchesACornerAndMissesWhenSeparated() {
        val hit = Aabb(3f, 4f, 10f, 10f)
        assertTrue(circleIntersectsAabb(0f, 0f, 5f, hit))
        assertFalse(circleIntersectsAabb(0f, 0f, 5f, Aabb(3.2f, 4.2f, 10f, 10f)))
        assertFalse(circleIntersectsAabb(0f, 0f, 5f, Aabb(4f, 2f, 3f, 6f)))
        assertFalse(circleIntersectsAabb(0f, 0f, -1f, hit))
    }

    @Test
    fun theOpenGapAndADeepCloudHitAreDistinct() {
        val tuning = Tuning()
        val gate = centeredGate(tuning, gapCenter = 540f, gapHeight = 400f)
        assertFalse(eyeHitsGate(tuning.eyeX, 540f, tuning.hitRadius, gate, tuning))
        assertTrue(eyeHitsGate(tuning.eyeX, 180f, tuning.hitRadius, gate, tuning))
    }

    @Test
    fun grazingTheVisualCloudMissesTheSolidCore() {
        val tuning = Tuning()
        val gate = centeredGate(tuning, gapCenter = 540f, gapHeight = 400f)
        val visualTop = 540f - 200f
        val solidBottom = visualTop - tuning.gapForgiveness
        val hit = tuning.hitRadius
        val grazeY = solidBottom + hit + 4f
        assertTrue(grazeY < visualTop)
        assertFalse(eyeHitsGate(tuning.eyeX, grazeY, hit, gate, tuning))
        assertTrue(eyeHitsGate(tuning.eyeX, solidBottom - 20f, hit, gate, tuning))
    }

    @Test
    fun grazingTheSideFluffMissesUntilTheCore() {
        val tuning = Tuning()
        val gate = centeredGate(tuning, gapCenter = 540f, gapHeight = 400f)
        val solidLeft = gate.x + tuning.sideForgiveness
        val solidRight = gate.x + gate.width - tuning.sideForgiveness
        val hit = tuning.hitRadius
        val grazeX = solidLeft - hit - 4f
        val insideY = 180f
        assertTrue(grazeX > gate.x)
        assertFalse(eyeHitsGate(grazeX, insideY, hit, gate, tuning))
        assertTrue(eyeHitsGate((solidLeft + solidRight) / 2f, insideY, hit, gate, tuning))
    }

    @Test
    fun easyGapsAreWiderAndSlowerThanNormalAndNormalCaps() {
        val tuning = Tuning()
        assertTrue(tuning.easy.baseGap > tuning.normal.baseGap)
        assertTrue(tuning.easy.baseSpeed < tuning.normal.baseSpeed)
        assertEqualsSpeed(tuning.easy.baseSpeed, speedFor(tuning.easy, 0))
        assertEqualsSpeed(tuning.easy.baseSpeed, speedFor(tuning.easy, 80))
        assertEqualsSpeed(tuning.easy.baseGap, gapFor(tuning.easy, 80))
        assertTrue(speedFor(tuning.normal, tuning.normal.progressEvery) > tuning.normal.baseSpeed)
        assertTrue(gapFor(tuning.normal, tuning.normal.progressEvery) < tuning.normal.baseGap)
        assertEqualsSpeed(tuning.normal.maxSpeed, speedFor(tuning.normal, 100000))
        assertEqualsSpeed(tuning.normal.minGap, gapFor(tuning.normal, 100000))
        assertTrue(tuning.normal.minGap > tuning.eyeRadius * 4f)
        assertTrue(tuning.sideForgiveness > tuning.hitRadius)
    }

    private fun centeredGate(tuning: Tuning, gapCenter: Float, gapHeight: Float): CloudGate {
        return CloudGate(
            x = tuning.eyeX - tuning.gateWidth / 2f,
            gapCenterY = gapCenter,
            gapHeight = gapHeight,
            width = tuning.gateWidth,
            passed = false,
        )
    }

    private fun assertEqualsSpeed(expected: Float, actual: Float) {
        org.junit.Assert.assertEquals(expected, actual, 0.01f)
    }
}
