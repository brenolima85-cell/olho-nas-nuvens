package app.olhonasnuvens.tv.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryAnnouncerTest {
    @Test
    fun theSameSummaryIsNotPublishedAgain() {
        val announcer = SummaryAnnouncer()
        val session = GameSession()
        val summary = session.accessibilitySummary()
        assertEquals(summary, announcer.update(summary))
        assertNull(announcer.update(summary))
        assertNull(announcer.update(session.accessibilitySummary()))
    }

    @Test
    fun menuFocusPublishesEvenWhenScoreAndPhaseStay() {
        val announcer = SummaryAnnouncer()
        val session = GameSession()
        val easy = session.accessibilitySummary()
        announcer.update(easy)
        assertEquals(SessionPhase.TITLE, session.phase)
        assertEquals(0, session.score)
        session.onDown()
        val normal = session.accessibilitySummary()
        assertTrue(normal != easy)
        assertEquals(SessionPhase.TITLE, session.phase)
        assertEquals(0, session.score)
        assertEquals(normal, announcer.update(normal))
        assertTrue(normal.contains(Copy.NORMAL))
    }

    @Test
    fun scoreBestHelpAndRestoreEachPublish() {
        val announcer = SummaryAnnouncer()
        val session = GameSession()
        announcer.update(session.accessibilitySummary())
        session.onDown()
        session.onDown()
        session.onConfirm()
        val help = session.accessibilitySummary()
        assertEquals(help, announcer.update(help))
        assertTrue(help.contains(Copy.HELP_TITLE))

        session.onBack()
        session.onConfirm()
        val playing = session.capture().copy(score = 3, phase = SessionPhase.PLAYING)
        session.restore(playing)
        val live = session.accessibilitySummary()
        assertEquals(live, announcer.update(live))
        assertTrue(live.contains("3"))

        val over = session.capture().copy(
            phase = SessionPhase.GAME_OVER,
            score = 3,
            menuIndex = 0,
        )
        session.restore(over)
        val done = session.accessibilitySummary()
        assertTrue(done != live)
        assertEquals(done, announcer.update(done))
        assertTrue(done.contains(Copy.GAME_OVER))
        assertTrue(session.isGameOverConfirmBlocked)
    }
}
