package app.olhonasnuvens.tv.input

import app.olhonasnuvens.tv.game.BackResult
import app.olhonasnuvens.tv.game.Difficulty
import app.olhonasnuvens.tv.game.GameSession
import app.olhonasnuvens.tv.game.MenuAction
import app.olhonasnuvens.tv.game.SessionPhase
import app.olhonasnuvens.tv.game.SequenceGapSource
import app.olhonasnuvens.tv.killWithScore
import app.olhonasnuvens.tv.openGameOverRetry
import app.olhonasnuvens.tv.startNew
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameOverMashTest {
    @Test
    fun okRightAfterDeathKeepsGameOverScreen() {
        val s = GameSession(gaps = SequenceGapSource(List(50) { 480f }))
        val d = InputDecoder()
        var t = 1_000L
        fun press(playing: Boolean) = d.decode(TvKeyCodes.DPAD_CENTER, 0, t, playing)
        // começa no modo fácil
        (press(false) as InputOutcome.Action).let { SessionInput.apply(it.action, s) }
        assertEquals(SessionPhase.PLAYING, s.phase)
        // jogador para de apertar e cai até o chão
        var frames = 0
        while (s.phase == SessionPhase.PLAYING && frames < 600) { s.fixedUpdate(); frames++; t += 16 }
        assertEquals(SessionPhase.GAME_OVER, s.phase)
        // um OK 50 ms depois da morte (criança apertando sem parar)
        t += 50
        val out = press(s.phase == SessionPhase.PLAYING)
        if (out is InputOutcome.Action) SessionInput.apply(out.action, s)
        assertEquals(SessionPhase.GAME_OVER, s.phase)
    }

    @Test
    fun aLaterConfirmRetriesTheSameEasyRun() {
        val session = died(Difficulty.EASY)
        assertEquals(MenuAction.RETRY, session.menu().items.first().action)
        session.onConfirm()
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        openGameOverRetry(session)
        session.onConfirm()
        assertEquals(SessionPhase.PLAYING, session.phase)
        assertEquals(Difficulty.EASY, session.difficulty)
        assertEquals(0, session.score)
        assertEquals(session.tuning.eyeStartY, session.eyeY, 0.001f)
    }

    @Test
    fun normalModeUsesTheSameShortLock() {
        val session = died(Difficulty.NORMAL)
        session.onConfirm()
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        assertEquals(Difficulty.NORMAL, session.difficulty)
        openGameOverRetry(session)
        session.onConfirm()
        assertEquals(SessionPhase.PLAYING, session.phase)
        assertEquals(Difficulty.NORMAL, session.difficulty)
        assertEquals(0, session.score)
    }

    @Test
    fun aSingleLargeStepCannotSkipTheLock() {
        val session = died(Difficulty.EASY)
        session.advanceGameOverClock(5f)
        session.advanceGameOverClock(-1f)
        assertTrue(session.isGameOverConfirmBlocked)
        session.onConfirm()
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        session.advanceGameOverClock(5f)
        session.advanceGameOverClock(5f)
        assertFalse(session.isGameOverConfirmBlocked)
    }

    @Test
    fun heldRepeatDoesNotRetryAndAFreshPressAfterTheLockDoes() {
        val session = died(Difficulty.EASY)
        val decoder = InputDecoder()
        val held = decoder.decode(TvKeyCodes.BUTTON_A, 6, 4_000L, playing = false)
        assertTrue(held is InputOutcome.Consumed)
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        assertTrue(holdsGameOverConfirm(session, TvKeyCodes.DPAD_CENTER))
        assertTrue(holdsGameOverConfirm(session, TvKeyCodes.BUTTON_A))
        assertTrue(holdsGameOverConfirm(session, TvKeyCodes.ENTER))
        assertFalse(holdsGameOverConfirm(session, TvKeyCodes.DPAD_DOWN))
        assertFalse(holdsGameOverConfirm(session, TvKeyCodes.BACK))
        openGameOverRetry(session)
        assertFalse(holdsGameOverConfirm(session, TvKeyCodes.BUTTON_A))
        val again = decoder.decode(TvKeyCodes.BUTTON_A, 4, 9_000L, playing = false)
        assertTrue(again is InputOutcome.Consumed)
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        val deliberate = decoder.decode(TvKeyCodes.BUTTON_A, 0, 9_000L, playing = false)
        assertTrue(deliberate is InputOutcome.Action)
        SessionInput.apply((deliberate as InputOutcome.Action).action, session)
        assertEquals(SessionPhase.PLAYING, session.phase)
    }

    @Test
    fun mashingConfirmDoesNotPoisonTheDeliberateRetry() {
        val session = died(Difficulty.EASY)
        val decoder = InputDecoder()
        assertTrue(holdsGameOverConfirm(session, TvKeyCodes.DPAD_CENTER))
        session.onConfirm()
        session.onConfirm()
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        assertEquals(0, session.menu().selected)
        openGameOverRetry(session)
        val press = decoder.decode(TvKeyCodes.DPAD_CENTER, 0, 2_000L, playing = false)
        SessionInput.apply((press as InputOutcome.Action).action, session)
        assertEquals(SessionPhase.PLAYING, session.phase)
    }

    @Test
    fun focusCanMoveDuringTheLockButConfirmWaits() {
        val session = died(Difficulty.EASY)
        session.onDown()
        session.onConfirm()
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        assertEquals(1, session.menu().selected)
        openGameOverRetry(session)
        session.onConfirm()
        assertEquals(SessionPhase.TITLE, session.phase)
    }

    @Test
    fun backLeavesImmediatelyAndPauseRetryDoesNotWait() {
        val over = died(Difficulty.EASY)
        assertEquals(BackResult.STAY, over.onBack())
        assertEquals(SessionPhase.TITLE, over.phase)

        val paused = startNew(Difficulty.NORMAL)
        paused.onBack()
        assertFalse(paused.isGameOverConfirmBlocked)
        paused.onDown()
        paused.onConfirm()
        assertEquals(SessionPhase.PLAYING, paused.phase)
        assertEquals(Difficulty.NORMAL, paused.difficulty)
        assertEquals(0, paused.score)
    }

    @Test
    fun restoreKeepsTheRemainingWaitAndAFinishedWaitStaysOpen() {
        val session = died(Difficulty.EASY)
        repeat(8) { session.advanceGameOverClock(session.tuning.fixedDt) }
        assertTrue(session.isGameOverConfirmBlocked)
        session.background()
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        val copy = GameSession()
        copy.restore(session.capture())
        copy.onConfirm()
        assertEquals(SessionPhase.GAME_OVER, copy.phase)
        openGameOverRetry(copy)
        copy.onConfirm()
        assertEquals(SessionPhase.PLAYING, copy.phase)

        val finished = died(Difficulty.NORMAL)
        openGameOverRetry(finished)
        val resumed = GameSession()
        resumed.restore(finished.capture())
        assertFalse(resumed.isGameOverConfirmBlocked)
        resumed.onConfirm()
        assertEquals(SessionPhase.PLAYING, resumed.phase)
        assertEquals(Difficulty.NORMAL, resumed.difficulty)
    }

    @Test
    fun aNewFlightLocksAgainAfterRetry() {
        val session = died(Difficulty.EASY)
        openGameOverRetry(session)
        session.onConfirm()
        killWithScore(session, 1)
        assertTrue(session.isGameOverConfirmBlocked)
        session.onConfirm()
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        assertEquals(Difficulty.EASY, session.difficulty)
    }

    private fun died(mode: Difficulty): GameSession {
        val session = startNew(mode)
        killWithScore(session, 2)
        assertTrue(session.isGameOverConfirmBlocked)
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        return session
    }
}
