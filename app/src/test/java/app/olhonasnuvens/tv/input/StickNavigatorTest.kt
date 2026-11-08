package app.olhonasnuvens.tv.input

import app.olhonasnuvens.tv.game.SessionPhase
import app.olhonasnuvens.tv.killWithScore
import app.olhonasnuvens.tv.startNew
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StickNavigatorTest {
    @Test
    fun holdDoesNotRepeatUntilTheStickReturnsToNeutral() {
        val stick = StickNavigator()
        assertEquals(StickDirection.UP, stick.onSample(-0.8f, 0f))
        repeat(30) { assertNull(stick.onSample(-0.9f, 0f)) }
        assertNull(stick.onSample(-0.35f, 0f))
        assertNull(stick.onSample(-0.8f, 0f))
        assertNull(stick.onSample(0f, 0f))
        assertEquals(StickDirection.UP, stick.onSample(-0.8f, 0f))
    }

    @Test
    fun theStickWinsOverTheHatAndTheHatWorksInsideTheDeadzone() {
        val stick = StickNavigator()
        assertNull(stick.onSample(0.5f, 0f))
        assertEquals(StickDirection.DOWN, stick.onSample(0.51f, -1f))
        assertNull(stick.onSample(0f, 0f))
        assertEquals(StickDirection.UP, stick.onSample(0.1f, -1f))
        assertNull(stick.onSample(0.1f, 0f))
    }

    @Test
    fun disarmIgnoresAStickThatIsStillHeld() {
        val stick = StickNavigator()
        assertEquals(StickDirection.DOWN, stick.onSample(0.9f, 0f))
        stick.disarm()
        assertNull(stick.onSample(0.9f, 0f))
        assertNull(stick.onSample(0.35f, 0f))
        assertNull(stick.onSample(0f, 0f))
        assertEquals(StickDirection.DOWN, stick.onSample(0.9f, 0f))
    }

    @Test
    fun moveSamplesAndCancelReleasesMatchThePlatformActions() {
        assertTrue(MotionActions.samplesAxes(MotionActions.MOVE))
        assertTrue(MotionActions.MOVE == 2)
        assertTrue(MotionActions.releasesHold(MotionActions.UP))
        assertTrue(MotionActions.releasesHold(MotionActions.CANCEL))
        assertTrue(!MotionActions.samplesAxes(MotionActions.DOWN))
        assertTrue(!MotionActions.releasesHold(MotionActions.MOVE))
    }

    @Test
    fun analogVerticalDoesNotFlapOrSkipTheDeathLock() {
        val session = startNew()
        val stick = StickNavigator()
        assertEquals(StickDirection.UP, stick.onSample(-0.9f, 0f))
        SessionInput.apply(PadAction.UP, session)
        assertEquals(0f, session.eyeVy, 0.001f)
        assertEquals(SessionPhase.PLAYING, session.phase)
        repeat(10) { assertNull(stick.onSample(-0.9f, 0f)) }
        killWithScore(session, 1)
        SessionInput.apply(PadAction.DOWN, session)
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        assertTrue(session.isGameOverConfirmBlocked)
        assertEquals(1, session.score)
    }

    @Test
    fun oneTiltMovesTheMenuASingleStep() {
        val session = app.olhonasnuvens.tv.game.GameSession()
        val stick = StickNavigator()
        var steps = 0
        repeat(20) {
            if (stick.onSample(0.95f, -1f) == StickDirection.DOWN) steps++
        }
        assertEquals(1, steps)
        SessionInput.apply(PadAction.DOWN, session)
        assertEquals(1, session.menu().selected)
        assertNull(stick.onSample(0f, 0f))
        assertEquals(StickDirection.DOWN, stick.onSample(0.95f, 0f))
        SessionInput.apply(PadAction.DOWN, session)
        assertEquals(2, session.menu().selected)
    }
}
