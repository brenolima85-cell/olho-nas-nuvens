package app.olhonasnuvens.tv.input

import app.olhonasnuvens.tv.game.SessionPhase
import app.olhonasnuvens.tv.startNew
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputTest {
    @Test
    fun confirmMapsTheRemoteAndTheGamepad() {
        assertEquals(PadAction.CONFIRM, classifyKey(TvKeyCodes.DPAD_CENTER))
        assertEquals(PadAction.CONFIRM, classifyKey(TvKeyCodes.ENTER))
        assertEquals(PadAction.CONFIRM, classifyKey(TvKeyCodes.BUTTON_A))
        assertEquals(PadAction.CONFIRM, classifyKey(TvKeyCodes.BUTTON_SELECT))
        assertEquals(PadAction.BACK, classifyKey(TvKeyCodes.BACK))
        assertEquals(PadAction.BACK, classifyKey(TvKeyCodes.BUTTON_B))
        assertEquals(PadAction.PAUSE, classifyKey(TvKeyCodes.BUTTON_START))
        assertEquals(PadAction.PAUSE, classifyKey(TvKeyCodes.MEDIA_PLAY_PAUSE))
        assertNull(classifyKey(999))
    }

    @Test
    fun aHeldOrDoubledConfirmFlapsOnlyOnce() {
        val decoder = InputDecoder()
        val session = startNew()
        val first = decoder.decode(TvKeyCodes.BUTTON_A, 0, 1_000L, playing = true)
        assertTrue(first is InputOutcome.Action)
        SessionInput.apply((first as InputOutcome.Action).action, session)
        session.fixedUpdate()
        val afterGravity = session.eyeVy
        assertTrue(afterGravity > session.tuning.flapVelocity)
        val doubled = decoder.decode(TvKeyCodes.DPAD_CENTER, 0, 1_040L, playing = true)
        val held = decoder.decode(TvKeyCodes.BUTTON_A, 4, 2_000L, playing = true)
        assertTrue(doubled is InputOutcome.Consumed)
        assertTrue(held is InputOutcome.Consumed)
        assertEquals(afterGravity, session.eyeVy, 0.001f)
        val later = decoder.decode(
            TvKeyCodes.ENTER,
            0,
            1_000L + InputDecoder.FLAP_INTERVAL_MS,
            playing = true,
        )
        assertTrue(later is InputOutcome.Action)
        SessionInput.apply((later as InputOutcome.Action).action, session)
        assertEquals(session.tuning.flapVelocity, session.eyeVy, 0.001f)
    }

    @Test
    fun gameOverConfirmIsNotBlockedByTheFlapDebounce() {
        val decoder = InputDecoder()
        val flap = decoder.decode(TvKeyCodes.BUTTON_A, 0, 1_000L, playing = true)
        assertTrue(flap is InputOutcome.Action)
        val retry = decoder.decode(TvKeyCodes.DPAD_CENTER, 0, 1_020L, playing = false)
        assertTrue(retry is InputOutcome.Action)
        assertEquals(PadAction.CONFIRM, (retry as InputOutcome.Action).action)
    }

    @Test
    fun menuRepeatsAreSwallowedAndLeftRightDoNothing() {
        val decoder = InputDecoder()
        val session = app.olhonasnuvens.tv.game.GameSession()
        val down = decoder.decode(TvKeyCodes.DPAD_DOWN, 0, 5_000L, playing = false)
        SessionInput.apply((down as InputOutcome.Action).action, session)
        assertEquals(1, session.menu().selected)
        val held = decoder.decode(TvKeyCodes.DPAD_DOWN, 3, 5_100L, playing = false)
        assertTrue(held is InputOutcome.Consumed)
        assertEquals(1, session.menu().selected)
        val step = decoder.decode(
            TvKeyCodes.DPAD_DOWN,
            0,
            5_000L + InputDecoder.NAV_INTERVAL_MS,
            playing = false,
        )
        SessionInput.apply((step as InputOutcome.Action).action, session)
        assertEquals(2, session.menu().selected)
        assertTrue(decoder.decode(TvKeyCodes.DPAD_LEFT, 0, 9_000L, playing = false) is InputOutcome.Consumed)
        assertTrue(decoder.decode(TvKeyCodes.DPAD_RIGHT, 0, 9_100L, playing = true) is InputOutcome.Consumed)
        assertEquals(2, session.menu().selected)
        assertTrue(decoder.decode(42, 0, 9_200L, playing = false) is InputOutcome.Unhandled)
    }

    @Test
    fun startPausesAndResumes() {
        val decoder = InputDecoder()
        val session = startNew()
        val pause = decoder.decode(TvKeyCodes.BUTTON_START, 0, 100L, playing = true)
        SessionInput.apply((pause as InputOutcome.Action).action, session)
        assertEquals(SessionPhase.PAUSED, session.phase)
        val resume = decoder.decode(TvKeyCodes.MEDIA_PLAY_PAUSE, 0, 400L, playing = false)
        SessionInput.apply((resume as InputOutcome.Action).action, session)
        assertEquals(SessionPhase.PLAYING, session.phase)
    }

    @Test
    fun aSecondBackInsideTheNavWindowDoesNotToggleTwice() {
        val decoder = InputDecoder()
        val session = startNew()
        val first = decoder.decode(TvKeyCodes.BACK, 0, 1_000L, playing = true)
        SessionInput.apply((first as InputOutcome.Action).action, session)
        assertEquals(SessionPhase.PAUSED, session.phase)
        val echoed = decoder.decode(TvKeyCodes.BACK, 0, 1_040L, playing = false)
        assertTrue(echoed is InputOutcome.Consumed)
        assertEquals(SessionPhase.PAUSED, session.phase)
        val later = decoder.decode(
            TvKeyCodes.BACK,
            0,
            1_000L + InputDecoder.NAV_INTERVAL_MS,
            playing = false,
        )
        SessionInput.apply((later as InputOutcome.Action).action, session)
        assertEquals(SessionPhase.PLAYING, session.phase)
    }

    @Test
    fun repeatGateRejectsRepeatsAndAcceptsAClockStepBack() {
        val gate = RepeatGate(100L)
        assertTrue(gate.accept(1_000L, 0))
        assertTrue(!gate.accept(1_050L, 0))
        assertTrue(!gate.accept(1_200L, 2))
        assertTrue(gate.accept(500L, 0))
        assertTrue(gate.accept(600L, 0))
    }
}
