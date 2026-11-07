package app.olhonasnuvens.tv.game

enum class Difficulty {
    EASY,
    NORMAL,
}

enum class SessionPhase {
    TITLE,
    PLAYING,
    PAUSED,
    GAME_OVER,
}

enum class BackResult {
    STAY,
    FINISH,
}

enum class MenuAction {
    START_EASY,
    START_NORMAL,
    OPEN_HELP,
    CLOSE_HELP,
    RESUME,
    RETRY,
    TO_TITLE,
}

data class MenuItem(
    val label: String,
    val action: MenuAction,
)

data class MenuState(
    val heading: String,
    val subtitle: String,
    val items: List<MenuItem>,
    val selected: Int,
    val lines: List<String>,
    val hint: String,
)

/** Banco de nuvem com um vão. O miolo sólido é menor que o desenho. */
data class CloudGate(
    val x: Float,
    val gapCenterY: Float,
    val gapHeight: Float,
    val width: Float,
    val passed: Boolean,
)

data class SimulationState(
    val phase: SessionPhase,
    val helpOpen: Boolean,
    val difficulty: Difficulty,
    val score: Int,
    val newRecord: Boolean,
    val eyeY: Float,
    val eyeVy: Float,
    val wingEnergy: Float,
    val aliveTime: Float,
    val menuIndex: Int,
    val nextSpawnX: Float,
    val bestEasy: Int,
    val bestNormal: Int,
    val gates: List<CloudGate>,
)

data class RenderFrame(
    val phase: SessionPhase,
    val helpOpen: Boolean,
    val difficulty: Difficulty,
    val score: Int,
    val best: Int,
    val newRecord: Boolean,
    val eyeX: Float,
    val eyeY: Float,
    val eyeVy: Float,
    val eyeRadius: Float,
    val wingEnergy: Float,
    val aliveTime: Float,
    val pupilLook: Float,
    val gates: List<CloudGate>,
    val menu: MenuState,
    val groundY: Float,
    val worldWidth: Float,
    val worldHeight: Float,
)
