package app.olhonasnuvens.tv.input

import app.olhonasnuvens.tv.game.GameSession
import app.olhonasnuvens.tv.game.SessionPhase
import app.olhonasnuvens.tv.game.SequenceGapSource
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

class GameOverMashTest {
    @Ignore("Bug conhecido: OK logo após morrer recomeça a partida. Remover o @Ignore quando houver a trava de ~700 ms.")
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
}
