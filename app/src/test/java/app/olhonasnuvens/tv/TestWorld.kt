package app.olhonasnuvens.tv

import app.olhonasnuvens.tv.game.Difficulty
import app.olhonasnuvens.tv.game.GameSession
import app.olhonasnuvens.tv.game.SequenceGapSource
import app.olhonasnuvens.tv.game.SessionPhase
import org.junit.Assert.assertEquals

internal fun startNew(mode: Difficulty = Difficulty.EASY): GameSession {
    val session = GameSession(gaps = SequenceGapSource(List(12) { 540f }))
    if (mode == Difficulty.NORMAL) session.onDown()
    session.onConfirm()
    assertEquals(SessionPhase.PLAYING, session.phase)
    assertEquals(mode, session.difficulty)
    return session
}

internal fun killWithScore(session: GameSession, score: Int) {
    val tuning = session.tuning
    session.restore(
        session.capture().copy(
            phase = SessionPhase.PLAYING,
            helpOpen = false,
            score = score,
            newRecord = false,
            eyeY = tuning.groundY,
            eyeVy = 0f,
            gates = emptyList(),
            nextSpawnX = 100000f,
        ),
    )
    session.fixedUpdate()
    assertEquals(SessionPhase.GAME_OVER, session.phase)
}
