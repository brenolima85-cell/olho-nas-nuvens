package app.olhonasnuvens.tv.game

import app.olhonasnuvens.tv.startNew
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StateCodecTest {
    @Test
    fun aSnapshotRoundTripsExactly() {
        val session = startNew(Difficulty.NORMAL)
        session.onBack()
        val encoded = StateCodec.encode(session.capture())
        val decoded = StateCodec.decode(encoded)
        assertEquals(session.capture(), decoded)
        val fresh = GameSession()
        fresh.restore(decoded!!)
        assertEquals(SessionPhase.PAUSED, fresh.phase)
        assertEquals(Difficulty.NORMAL, fresh.difficulty)
        assertEquals(session.gates(), fresh.gates())
        assertEquals(session.best.get(Difficulty.NORMAL), fresh.best.get(Difficulty.NORMAL))
    }

    @Test
    fun garbageAndANegativeScoreAreRejected() {
        assertNull(StateCodec.decode(""))
        assertNull(StateCodec.decode("v1\nTITLE"))
        assertNull(StateCodec.decode("nope"))
        val session = startNew()
        val lines = StateCodec.encode(session.capture()).split('\n').toMutableList()
        lines[4] = "-3"
        assertNull(StateCodec.decode(lines.joinToString("\n")))
    }
}
