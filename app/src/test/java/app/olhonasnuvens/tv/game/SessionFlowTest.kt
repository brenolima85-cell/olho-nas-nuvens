package app.olhonasnuvens.tv.game

import app.olhonasnuvens.tv.killWithScore
import app.olhonasnuvens.tv.startNew
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionFlowTest {
    @Test
    fun copyStaysEntertainmentOnlyAndProvisional() {
        assertEquals(
            "Entretenimento apenas. Não faz diagnóstico e não promete melhora da visão.",
            Copy.DISCLAIMER,
        )
        assertTrue(Copy.PROVISIONAL.contains("Clínica de Olhos"))
        assertTrue(Copy.PROVISIONAL.contains("marca provisória"))
        assertTrue(Copy.EASY_HINT.contains("lent"))
        assertTrue(Copy.NORMAL_HINT.contains("limite"))
    }

    @Test
    fun titleMenuDoesNotWrapAndHelpClosesOnBack() {
        val session = GameSession()
        assertEquals(0, session.menu().selected)
        assertEquals(MenuAction.START_EASY, session.menu().items[0].action)
        session.onUp()
        assertEquals(0, session.menu().selected)
        session.onDown()
        session.onDown()
        session.onDown()
        assertEquals(2, session.menu().selected)
        session.onConfirm()
        assertTrue(session.helpOpen)
        assertEquals(Copy.HELP_TITLE, session.menu().heading)
        assertTrue(session.accessibilitySummary().contains("não trata a visão"))
        assertEquals(BackResult.STAY, session.onBack())
        assertFalse(session.helpOpen)
        assertEquals(SessionPhase.TITLE, session.phase)
    }

    @Test
    fun backOnTheTitleAsksToCloseAndBackTogglesPause() {
        val title = GameSession()
        assertEquals(BackResult.FINISH, title.onBack())
        val session = startNew()
        assertEquals(BackResult.STAY, session.onBack())
        assertEquals(SessionPhase.PAUSED, session.phase)
        assertEquals(MenuAction.RESUME, session.menu().items[0].action)
        val vy = session.eyeVy
        session.onConfirm()
        assertEquals(SessionPhase.PLAYING, session.phase)
        assertEquals(vy, session.eyeVy, 0.001f)
        session.togglePause()
        assertEquals(SessionPhase.PAUSED, session.phase)
        session.togglePause()
        assertEquals(SessionPhase.PLAYING, session.phase)
        title.togglePause()
        assertEquals(SessionPhase.TITLE, title.phase)
    }

    @Test
    fun gameOverConfirmRetriesAndTheSecondItemReturnsToTitle() {
        val session = startNew()
        killWithScore(session, 2)
        assertEquals(0, session.menu().selected)
        assertEquals(MenuAction.RETRY, session.menu().items[0].action)
        assertTrue(session.accessibilitySummary().contains(Copy.GAME_OVER))
        session.onDown()
        session.onConfirm()
        assertEquals(SessionPhase.TITLE, session.phase)
        assertTrue(session.accessibilitySummary().contains(Copy.DISCLAIMER))
        assertTrue(session.accessibilitySummary().contains(Copy.PROVISIONAL))
    }

    @Test
    fun leavingTheForegroundPausesALiveRun() {
        val session = startNew()
        session.background()
        assertEquals(SessionPhase.PAUSED, session.phase)
        val y = session.eyeY
        session.fixedUpdate()
        assertEquals(y, session.eyeY, 0.001f)
        session.onConfirm()
        assertEquals(SessionPhase.PLAYING, session.phase)
    }

    @Test
    fun pauseRetryStartsTheSameModeOver() {
        val session = startNew(Difficulty.NORMAL)
        session.onBack()
        session.onDown()
        assertEquals(MenuAction.RETRY, session.menu().items[session.menu().selected].action)
        session.onConfirm()
        assertEquals(SessionPhase.PLAYING, session.phase)
        assertEquals(Difficulty.NORMAL, session.difficulty)
        assertEquals(0, session.score)
    }
}
