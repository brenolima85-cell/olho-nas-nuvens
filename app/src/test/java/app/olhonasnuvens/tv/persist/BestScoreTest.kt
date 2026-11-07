package app.olhonasnuvens.tv.persist

import app.olhonasnuvens.tv.game.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Test

class BestScoreTest {
    @Test
    fun recordsStaySeparateAndIgnoreNegativeScores() {
        val board = BestScoreBoard()
        assertEquals(0, board.get(Difficulty.EASY))
        assertEquals(4, board.submit(Difficulty.EASY, 4))
        assertEquals(4, board.submit(Difficulty.EASY, 2))
        assertEquals(4, board.submit(Difficulty.EASY, -8))
        assertEquals(6, board.submit(Difficulty.NORMAL, 6))
        assertEquals(4, board.get(Difficulty.EASY))
        board.replace(-3, 1)
        assertEquals(0, board.get(Difficulty.EASY))
        assertEquals(1, board.get(Difficulty.NORMAL))
    }
}
