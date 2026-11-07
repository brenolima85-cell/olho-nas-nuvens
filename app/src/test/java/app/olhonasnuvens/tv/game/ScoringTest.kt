package app.olhonasnuvens.tv.game

import app.olhonasnuvens.tv.killWithScore
import app.olhonasnuvens.tv.startNew
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {
    @Test
    fun clearingGapsScoresOnceEach() {
        val session = startNew()
        val tuning = session.tuning
        val width = tuning.gateWidth
        val gates = listOf(
            openGate(tuning.eyeX - width - 1f, tuning),
            openGate(tuning.eyeX - width - 40f, tuning),
        )
        session.restore(
            session.capture().copy(
                score = 0,
                eyeY = tuning.eyeStartY,
                eyeVy = 0f,
                aliveTime = 10f,
                gates = gates,
                nextSpawnX = 100000f,
            ),
        )
        assertEquals(0, session.score)
        session.fixedUpdate()
        assertEquals(SessionPhase.PLAYING, session.phase)
        assertEquals(2, session.score)
        session.fixedUpdate()
        session.fixedUpdate()
        assertEquals(2, session.score)
    }

    @Test
    fun hittingACloudEndsTheRunWithoutAnExtraPoint() {
        val session = startNew()
        val tuning = session.tuning
        val gate = CloudGate(
            x = tuning.eyeX - 30f,
            gapCenterY = tuning.eyeStartY + 420f,
            gapHeight = 180f,
            width = tuning.gateWidth,
            passed = false,
        )
        assertTrue(eyeHitsGate(tuning.eyeX, tuning.eyeStartY, tuning.hitRadius, gate, tuning))
        session.restore(
            session.capture().copy(
                score = 5,
                eyeY = tuning.eyeStartY,
                eyeVy = 0f,
                aliveTime = 10f,
                gates = listOf(gate),
                nextSpawnX = 100000f,
            ),
        )
        session.fixedUpdate()
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        assertEquals(5, session.score)
        assertEquals(5, session.best.get(Difficulty.EASY))
        assertTrue(session.newRecord)
    }

    @Test
    fun bestScoreIsKeptPerModeAndDoesNotFall() {
        val easy = startNew(Difficulty.EASY)
        killWithScore(easy, 4)
        assertTrue(easy.newRecord)
        assertEquals(4, easy.best.get(Difficulty.EASY))
        easy.onConfirm()
        assertEquals(SessionPhase.PLAYING, easy.phase)
        assertEquals(0, easy.score)
        assertEquals(Difficulty.EASY, easy.difficulty)
        killWithScore(easy, 4)
        assertFalse(easy.newRecord)
        assertEquals(4, easy.best.get(Difficulty.EASY))
        killWithScore(easy, 2)
        assertFalse(easy.newRecord)
        assertEquals(4, easy.best.get(Difficulty.EASY))

        val normal = startNew(Difficulty.NORMAL)
        normal.restore(easy.capture().copy(phase = SessionPhase.TITLE, helpOpen = false, menuIndex = 0))
        normal.onDown()
        normal.onConfirm()
        assertEquals(Difficulty.NORMAL, normal.difficulty)
        killWithScore(normal, 3)
        assertEquals(3, normal.best.get(Difficulty.NORMAL))
        assertEquals(4, normal.best.get(Difficulty.EASY))
        killWithScore(normal, 1)
        assertEquals(3, normal.best.get(Difficulty.NORMAL))
    }

    @Test
    fun retryResetsTheFlightAndKeepsTheRecord() {
        val session = startNew(Difficulty.NORMAL)
        killWithScore(session, 6)
        assertEquals(6, session.best.get(Difficulty.NORMAL))
        session.onConfirm()
        assertEquals(SessionPhase.PLAYING, session.phase)
        assertEquals(0, session.score)
        assertEquals(session.tuning.eyeStartY, session.eyeY, 0.001f)
        assertEquals(0f, session.eyeVy, 0.001f)
        assertEquals(0f, session.aliveTime, 0.001f)
        assertEquals(Difficulty.NORMAL, session.difficulty)
        assertEquals(6, session.best.get(Difficulty.NORMAL))
        assertEquals(3, session.gates().size)
        assertTrue(session.gates().all { it.gapHeight == session.tuning.normal.baseGap })
    }

    @Test
    fun newGatesUseTheProgressedNormalGap() {
        val session = GameSession(gaps = SequenceGapSource(List(8) { 500f }))
        session.onDown()
        session.onConfirm()
        val tuning = session.tuning
        val target = tuning.normal.progressEvery
        session.restore(
            session.capture().copy(
                score = target,
                gates = emptyList(),
                nextSpawnX = tuning.worldWidth + tuning.spawnAhead - 1f,
                eyeY = tuning.eyeStartY,
                eyeVy = 0f,
                aliveTime = 10f,
            ),
        )
        session.fixedUpdate()
        assertEquals(SessionPhase.PLAYING, session.phase)
        assertTrue(session.gates().isNotEmpty())
        assertEquals(gapFor(tuning.normal, target), session.gates().last().gapHeight, 0.01f)
    }

    @Test
    fun easySpeedStaysFlatAfterPoints() {
        val session = startNew()
        session.restore(session.capture().copy(score = 30, gates = emptyList(), nextSpawnX = 100000f))
        assertEquals(session.tuning.easy.baseSpeed, session.scrollSpeed(), 0.01f)
    }

    @Test
    fun theFirstGatesUseTheRequestedCentersAndEasyGap() {
        val centers = listOf(410f, 500f, 610f)
        val session = GameSession(gaps = SequenceGapSource(centers))
        session.onConfirm()
        assertEquals(centers, session.gates().map { it.gapCenterY })
        assertTrue(session.gates().all { it.gapHeight == session.tuning.easy.baseGap })
        assertTrue(session.scrollSpeed() < session.tuning.normal.baseSpeed)
    }

    private fun openGate(x: Float, tuning: Tuning): CloudGate {
        return CloudGate(
            x = x,
            gapCenterY = tuning.eyeStartY,
            gapHeight = 640f,
            width = tuning.gateWidth,
            passed = false,
        )
    }
}
