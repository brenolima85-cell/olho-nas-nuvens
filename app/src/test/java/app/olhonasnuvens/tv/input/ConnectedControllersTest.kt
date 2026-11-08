package app.olhonasnuvens.tv.input

import app.olhonasnuvens.tv.game.SessionPhase
import app.olhonasnuvens.tv.killWithScore
import app.olhonasnuvens.tv.startNew
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectedControllersTest {
    @Test
    fun onlyRemoteAndGamepadSourcesAreTracked() {
        val watch = ConnectedControllers()
        watch.update(3, PlayControllerSources.KEYBOARD)
        watch.update(4, PlayControllerSources.MOUSE)
        watch.update(5, PlayControllerSources.KEYBOARD or PlayControllerSources.MOUSE)
        assertFalse(watch.tracks(3))
        assertFalse(watch.tracks(4))
        assertFalse(watch.tracks(5))
        assertFalse(watch.remove(3))

        watch.update(6, PlayControllerSources.DPAD or PlayControllerSources.KEYBOARD)
        watch.update(7, PlayControllerSources.GAMEPAD or PlayControllerSources.KEYBOARD)
        watch.update(8, PlayControllerSources.JOYSTICK)
        assertTrue(watch.tracks(6))
        assertTrue(watch.tracks(7))
        assertTrue(watch.tracks(8))
        assertTrue(PlayControllerSources.isStickSource(PlayControllerSources.GAMEPAD))
        assertFalse(PlayControllerSources.isStickSource(PlayControllerSources.DPAD))
    }

    @Test
    fun removalPausesOnceAndReconnectDoesNotResume() {
        val watch = ConnectedControllers()
        val pad = 11
        watch.update(pad, PlayControllerSources.GAMEPAD)
        val session = startNew()
        assertTrue(watch.remove(pad))
        session.background()
        assertEquals(SessionPhase.PAUSED, session.phase)
        watch.update(pad, PlayControllerSources.GAMEPAD)
        session.background()
        assertEquals(SessionPhase.PAUSED, session.phase)
        assertFalse(watch.remove(99))
    }

    @Test
    fun losingTheGamepadSourcePausesAndGameOverKeepsItsLock() {
        val watch = ConnectedControllers()
        watch.update(2, PlayControllerSources.JOYSTICK)
        assertTrue(watch.update(2, PlayControllerSources.MOUSE).lostPlayController)
        assertFalse(watch.tracks(2))

        val session = startNew()
        killWithScore(session, 4)
        watch.update(2, PlayControllerSources.GAMEPAD)
        assertTrue(watch.remove(2))
        session.background()
        assertEquals(SessionPhase.GAME_OVER, session.phase)
        assertTrue(session.isGameOverConfirmBlocked)
        assertEquals(4, session.score)
    }
}
