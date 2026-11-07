package app.olhonasnuvens.tv.input

/**
 * Rejeita repetição automática da tecla e um segundo evento quase imediato
 * (alguns controles entregam centro e Enter no mesmo aperto).
 */
class RepeatGate(
    private val minIntervalMillis: Long,
) {
    private var lastAccepted = Long.MIN_VALUE / 4

    fun accept(nowMillis: Long, repeatCount: Int): Boolean {
        if (repeatCount > 0) return false
        if (nowMillis < lastAccepted) {
            lastAccepted = nowMillis
            return true
        }
        if (nowMillis - lastAccepted < minIntervalMillis) return false
        lastAccepted = nowMillis
        return true
    }
}
