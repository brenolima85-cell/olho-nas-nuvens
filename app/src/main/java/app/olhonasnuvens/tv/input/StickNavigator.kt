package app.olhonasnuvens.tv.input

import kotlin.math.abs

enum class StickDirection {
    UP,
    DOWN,
}

/**
 * Eixo vertical do analógico ou do hat. Um desvio além de [ENGAGE] gera um
 * único passo de menu. Segurar não repete. Voltar à zona neutra rearmar.
 * O analógico não sobe o olho: quem chama decide, e durante o jogo o passo é ignorado.
 */
class StickNavigator {
    private var latched: StickDirection? = null
    private var waitForNeutral: Boolean = false

    fun onSample(y: Float, hatY: Float): StickDirection? {
        if (!y.isFinite() || !hatY.isFinite()) {
            disarm()
            return null
        }
        val axis = if (abs(y) > ENGAGE) y else hatY
        val reading = when {
            axis <= -ENGAGE -> StickDirection.UP
            axis >= ENGAGE -> StickDirection.DOWN
            abs(axis) <= RELEASE -> null
            else -> latched
        }
        if (reading == null) {
            if (abs(axis) <= RELEASE) {
                latched = null
                waitForNeutral = false
            }
            return null
        }
        if (waitForNeutral) {
            latched = reading
            return null
        }
        if (reading == latched) return null
        latched = reading
        return reading
    }

    /** Depois de pausa ou desconexão, um eixo ainda preso não dispara sozinho. */
    fun disarm() {
        latched = null
        waitForNeutral = true
    }

    companion object {
        const val ENGAGE = 0.5f
        const val RELEASE = 0.2f
    }
}

object MotionActions {
    const val DOWN = 0
    const val UP = 1
    const val MOVE = 2
    const val CANCEL = 3

    fun samplesAxes(action: Int): Boolean = action == MOVE

    fun releasesHold(action: Int): Boolean = action == UP || action == CANCEL
}
