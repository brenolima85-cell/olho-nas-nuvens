package app.olhonasnuvens.tv.game

import kotlin.random.Random

fun interface GapCenterSource {
    fun next(gapHeight: Float, tuning: Tuning): Float
}

class RandomGapSource(
    private val random: Random,
) : GapCenterSource {
    override fun next(gapHeight: Float, tuning: Tuning): Float {
        val half = gapHeight / 2f
        val min = EDGE_MARGIN + half
        val max = tuning.groundY - EDGE_MARGIN - half
        if (max <= min) return (min + max) / 2f
        return min + random.nextFloat() * (max - min)
    }

    private companion object {
        const val EDGE_MARGIN = 90f
    }
}

class SequenceGapSource(
    values: List<Float>,
) : GapCenterSource {
    private val pending = ArrayDeque(values)

    override fun next(gapHeight: Float, tuning: Tuning): Float {
        check(pending.isNotEmpty()) { "SequenceGapSource esgotou os centros de vão" }
        return pending.removeFirst()
    }
}
