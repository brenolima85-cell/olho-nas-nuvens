package app.olhonasnuvens.tv.game

import app.olhonasnuvens.tv.killWithScore
import app.olhonasnuvens.tv.startNew
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicsTest {
    @Test
    fun gravityPullsDownMoreGentlyDuringTheOpeningGrace() {
        val session = startNew()
        session.fixedUpdate()
        val tuning = session.tuning
        val expected = tuning.gravity * tuning.graceGravityScale * tuning.fixedDt
        assertEquals(expected, session.eyeVy, 0.001f)
        assertTrue(session.eyeY > tuning.eyeStartY)
        assertEquals(SessionPhase.PLAYING, session.phase)
    }

    @Test
    fun gravityUsesFullStrengthAfterTheGrace() {
        val session = startNew()
        val tuning = session.tuning
        session.restore(
            session.capture().copy(
                aliveTime = tuning.graceSeconds,
                eyeVy = 0f,
                eyeY = tuning.eyeStartY,
                gates = emptyList(),
                nextSpawnX = 100000f,
            ),
        )
        session.fixedUpdate()
        assertEquals(tuning.gravity * tuning.fixedDt, session.eyeVy, 0.02f)
    }

    @Test
    fun flapSetsAnUpwardVelocityThatTheNextStepClimbs() {
        val session = startNew()
        session.onConfirm()
        assertEquals(session.tuning.flapVelocity, session.eyeVy, 0.001f)
        assertTrue(session.eyeVy < 0f)
        val before = session.eyeY
        session.fixedUpdate()
        assertTrue(session.eyeY < before)
    }

    @Test
    fun fallSpeedIsCapped() {
        val session = startNew()
        val tuning = session.tuning
        session.restore(
            session.capture().copy(
                eyeVy = 5000f,
                eyeY = 400f,
                aliveTime = 10f,
                gates = emptyList(),
                nextSpawnX = 100000f,
            ),
        )
        session.fixedUpdate()
        assertEquals(tuning.maxFallSpeed, session.eyeVy, 0.01f)
        assertEquals(SessionPhase.PLAYING, session.phase)
    }

    @Test
    fun ceilingIsASoftStopAndDoesNotEndTheRun() {
        val session = startNew()
        val tuning = session.tuning
        session.restore(
            session.capture().copy(
                eyeY = 0f,
                eyeVy = -400f,
                gates = emptyList(),
                nextSpawnX = 100000f,
            ),
        )
        session.fixedUpdate()
        assertEquals(tuning.eyeRadius + tuning.ceilingPadding, session.eyeY, 0.001f)
        assertEquals(0f, session.eyeVy, 0.001f)
        assertEquals(SessionPhase.PLAYING, session.phase)
    }

    @Test
    fun pausedAndBackgroundedRunsDoNotSimulate() {
        val session = startNew()
        session.onBack()
        assertEquals(SessionPhase.PAUSED, session.phase)
        val y = session.eyeY
        val vy = session.eyeVy
        session.fixedUpdate()
        assertEquals(y, session.eyeY, 0.001f)
        assertEquals(vy, session.eyeVy, 0.001f)

        val title = GameSession()
        title.background()
        assertEquals(SessionPhase.TITLE, title.phase)
        session.restore(session.capture().copy(phase = SessionPhase.GAME_OVER))
        session.background()
        assertEquals(SessionPhase.GAME_OVER, session.phase)
    }

    @Test
    fun fallingToTheGroundEndsTheRun() {
        val session = startNew()
        var guard = 0
        while (session.phase == SessionPhase.PLAYING && guard < 5000) {
            session.fixedUpdate()
            guard++
        }
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        assertTrue(guard < 5000)
        val tuning = session.tuning
        assertTrue(session.eyeY + tuning.hitRadius >= tuning.groundY - 1f)
    }

    @Test
    fun upOnTheDpadDoesNotFlapDuringPlay() {
        val session = startNew()
        session.onUp()
        assertEquals(0f, session.eyeVy, 0.001f)
        assertEquals(SessionPhase.PLAYING, session.phase)
    }

    @Test
    fun zeroScoreOnTheGroundIsNotARecord() {
        val session = startNew()
        killWithScore(session, 0)
        assertFalse(session.newRecord)
        assertEquals(0, session.best.get(Difficulty.EASY))
    }
}
