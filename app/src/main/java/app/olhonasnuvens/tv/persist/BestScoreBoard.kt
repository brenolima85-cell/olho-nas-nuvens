package app.olhonasnuvens.tv.persist

import app.olhonasnuvens.tv.game.Difficulty

/** Recorde local por modo. Não guarda nome, data clínica nem outro dado. */
class BestScoreBoard(
    easy: Int = 0,
    normal: Int = 0,
) {
    private var easyScore = easy.coerceAtLeast(0)
    private var normalScore = normal.coerceAtLeast(0)

    fun get(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> easyScore
        Difficulty.NORMAL -> normalScore
    }

    fun submit(difficulty: Difficulty, score: Int): Int {
        val updated = maxOf(get(difficulty), score.coerceAtLeast(0))
        when (difficulty) {
            Difficulty.EASY -> easyScore = updated
            Difficulty.NORMAL -> normalScore = updated
        }
        return updated
    }

    fun replace(easy: Int, normal: Int) {
        easyScore = easy.coerceAtLeast(0)
        normalScore = normal.coerceAtLeast(0)
    }
}
