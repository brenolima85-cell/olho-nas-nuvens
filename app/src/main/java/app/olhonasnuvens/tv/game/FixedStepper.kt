package app.olhonasnuvens.tv.game

/**
 * Converte o tempo do quadro em passos fixos. Um tranco grande não
 * teletransporta o olho: o excesso além de [maxSteps] é descartado.
 */
class FixedStepper(
    private val fixedDt: Float,
    private val maxFrameSeconds: Float,
    private val maxSteps: Int,
) {
    private var lastFrameNs: Long = 0L
    private var accumulator = 0f

    fun reset() {
        lastFrameNs = 0L
        accumulator = 0f
    }

    fun advanceNanos(frameNs: Long): Int {
        if (lastFrameNs == 0L) {
            lastFrameNs = frameNs
            return 0
        }
        var seconds = (frameNs - lastFrameNs) / NANOS_PER_SECOND
        lastFrameNs = frameNs
        if (seconds < 0f) seconds = 0f
        return advanceSeconds(seconds)
    }

    fun advanceSeconds(rawDt: Float): Int {
        var dt = rawDt
        if (dt < 0f) dt = 0f
        if (dt > maxFrameSeconds) dt = maxFrameSeconds
        accumulator += dt
        var steps = 0
        while (accumulator >= fixedDt && steps < maxSteps) {
            accumulator -= fixedDt
            steps++
        }
        if (steps == maxSteps && accumulator > fixedDt) {
            accumulator = 0f
        }
        return steps
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000f
    }
}
