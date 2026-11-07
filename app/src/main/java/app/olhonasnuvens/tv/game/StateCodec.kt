package app.olhonasnuvens.tv.game

/** Estado da partida em texto, para sobreviver à morte do processo. */
object StateCodec {
    private const val VERSION = "v1"
    private const val HEADER_LINES = 15
    private const val MAX_GATES = 16

    fun encode(state: SimulationState): String {
        val lines = ArrayList<String>(HEADER_LINES + state.gates.size)
        lines += VERSION
        lines += state.phase.name
        lines += state.helpOpen.toString()
        lines += state.difficulty.name
        lines += state.score.toString()
        lines += state.newRecord.toString()
        lines += bits(state.eyeY)
        lines += bits(state.eyeVy)
        lines += bits(state.wingEnergy)
        lines += bits(state.aliveTime)
        lines += state.menuIndex.toString()
        lines += bits(state.nextSpawnX)
        lines += state.bestEasy.toString()
        lines += state.bestNormal.toString()
        lines += state.gates.size.toString()
        for (gate in state.gates) {
            lines += listOf(
                bits(gate.x),
                bits(gate.gapCenterY),
                bits(gate.gapHeight),
                bits(gate.width),
                gate.passed.toString(),
            ).joinToString(",")
        }
        return lines.joinToString("\n")
    }

    fun decode(raw: String): SimulationState? {
        val lines = raw.split('\n')
        if (lines.size < HEADER_LINES || lines[0] != VERSION) return null
        val phase = enumOrNull<SessionPhase>(lines[1]) ?: return null
        val helpOpen = lines[2].toBooleanStrictOrNull() ?: return null
        val difficulty = enumOrNull<Difficulty>(lines[3]) ?: return null
        val score = lines[4].toIntOrNull() ?: return null
        if (score < 0) return null
        val newRecord = lines[5].toBooleanStrictOrNull() ?: return null
        val eyeY = bitsOrNull(lines[6]) ?: return null
        val eyeVy = bitsOrNull(lines[7]) ?: return null
        val wingEnergy = bitsOrNull(lines[8]) ?: return null
        val aliveTime = bitsOrNull(lines[9]) ?: return null
        val menuIndex = lines[10].toIntOrNull() ?: return null
        if (menuIndex < 0) return null
        val nextSpawnX = bitsOrNull(lines[11]) ?: return null
        val bestEasy = lines[12].toIntOrNull() ?: return null
        val bestNormal = lines[13].toIntOrNull() ?: return null
        if (bestEasy < 0 || bestNormal < 0) return null
        val count = lines[14].toIntOrNull() ?: return null
        if (count < 0 || count > MAX_GATES || lines.size != HEADER_LINES + count) return null
        val gates = ArrayList<CloudGate>(count)
        for (index in 0 until count) {
            val parts = lines[HEADER_LINES + index].split(',')
            if (parts.size != 5) return null
            val x = bitsOrNull(parts[0]) ?: return null
            val center = bitsOrNull(parts[1]) ?: return null
            val gap = bitsOrNull(parts[2]) ?: return null
            val width = bitsOrNull(parts[3]) ?: return null
            val passed = parts[4].toBooleanStrictOrNull() ?: return null
            if (!x.isFinite() || !center.isFinite() || !gap.isFinite() || !width.isFinite()) return null
            if (gap <= 0f || width <= 0f) return null
            gates += CloudGate(x, center, gap, width, passed)
        }
        if (!eyeY.isFinite() || !eyeVy.isFinite() || !wingEnergy.isFinite() ||
            !aliveTime.isFinite() || !nextSpawnX.isFinite()
        ) {
            return null
        }
        return SimulationState(
            phase = phase,
            helpOpen = helpOpen,
            difficulty = difficulty,
            score = score,
            newRecord = newRecord,
            eyeY = eyeY,
            eyeVy = eyeVy,
            wingEnergy = wingEnergy,
            aliveTime = aliveTime,
            menuIndex = menuIndex,
            nextSpawnX = nextSpawnX,
            bestEasy = bestEasy,
            bestNormal = bestNormal,
            gates = gates,
        )
    }

    private fun bits(value: Float): String = Integer.toUnsignedString(value.toRawBits(), 16)

    private fun bitsOrNull(raw: String): Float? {
        val parsed = raw.toLongOrNull(16) ?: return null
        if (parsed !in 0L..0xFFFF_FFFFL) return null
        return Float.fromBits(parsed.toInt())
    }

    private inline fun <reified T : Enum<T>> enumOrNull(raw: String): T? {
        return enumValues<T>().firstOrNull { it.name == raw }
    }
}
