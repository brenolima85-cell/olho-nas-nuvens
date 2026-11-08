package app.olhonasnuvens.tv.game

import kotlin.math.PI
import kotlin.math.sin

/**
 * Piscar do mascote: um fechamento curto a cada vários segundos.
 * Não é cintilação de tela. O restante do quadro não pulsa.
 */
object Motion {
    const val BLINK_CYCLE_SECONDS = 3.6f
    const val BLINK_DURATION_SECONDS = 0.18f

    /** 1 = olho aberto, 0 = pálpebra fechada no meio do piscar. */
    fun eyelidOpen(timeSeconds: Float): Float {
        if (timeSeconds <= 0f) return 1f
        val cycle = BLINK_CYCLE_SECONDS
        val duration = BLINK_DURATION_SECONDS
        val moment = timeSeconds % cycle
        val start = cycle - duration
        if (moment < start) return 1f
        val u = ((moment - start) / duration).coerceIn(0f, 1f)
        val wave = sin(u * PI.toFloat())
        return (1f - wave).coerceIn(0f, 1f)
    }
}
