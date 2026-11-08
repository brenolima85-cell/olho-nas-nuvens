package app.olhonasnuvens.tv.persist

import android.content.Context
import app.olhonasnuvens.tv.game.Difficulty

/**
 * Único dado gravado no aparelho: dois inteiros de recorde.
 * Backup desligado no manifesto. Sem conta e sem dado de paciente.
 */
object LocalBestScores {
    private const val PREFS = "olho_nas_nuvens_best"
    private const val KEY_EASY = "best_easy"
    private const val KEY_NORMAL = "best_normal"

    fun load(context: Context): BestScoreBoard {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return BestScoreBoard(
            easy = prefs.getInt(KEY_EASY, 0),
            normal = prefs.getInt(KEY_NORMAL, 0),
        )
    }

    fun save(context: Context, board: BestScoreBoard) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_EASY, board.get(Difficulty.EASY))
            .putInt(KEY_NORMAL, board.get(Difficulty.NORMAL))
            .apply()
    }
}
