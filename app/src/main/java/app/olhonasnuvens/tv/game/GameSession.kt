package app.olhonasnuvens.tv.game

import app.olhonasnuvens.tv.persist.BestScoreBoard

/**
 * Regras da partida. Sem Android, para os testes de física, pontos e reinício.
 * Usar só na thread de interface.
 */
class GameSession(
    val tuning: Tuning = Tuning(),
    private val gaps: GapCenterSource = RandomGapSource(kotlin.random.Random.Default),
    val best: BestScoreBoard = BestScoreBoard(),
) {
    var phase: SessionPhase = SessionPhase.TITLE
        private set
    var helpOpen: Boolean = false
        private set
    var difficulty: Difficulty = Difficulty.EASY
        private set
    var score: Int = 0
        private set
    var newRecord: Boolean = false
        private set
    var eyeY: Float = tuning.eyeStartY
        private set
    var eyeVy: Float = 0f
        private set
    var wingEnergy: Float = 0.25f
        private set
    var aliveTime: Float = 0f
        private set

    private var menuIndex: Int = 0
    private val gates = mutableListOf<CloudGate>()
    private var nextSpawnX: Float = tuning.worldWidth + tuning.initialLead
    private var gameOverElapsed: Float = 0f

    /**
     * OK/A do fim de jogo fica parado até a tela ter ficado visível por
     * [GAME_OVER_CONFIRM_LOCK_SECONDS]. O tempo é só de simulação, somado
     * em passos fixos enquanto o fim de jogo está em primeiro plano.
     */
    val isGameOverConfirmBlocked: Boolean
        get() = phase == SessionPhase.GAME_OVER &&
            gameOverElapsed < GAME_OVER_CONFIRM_LOCK_SECONDS

    fun gates(): List<CloudGate> = gates.toList()

    fun scrollSpeed(): Float = speedFor(tuning.spec(difficulty), score)

    fun menu(): MenuState {
        val items = itemsFor()
        val selected = if (items.isEmpty()) 0 else menuIndex.coerceIn(0, items.lastIndex)
        return MenuState(
            heading = heading(),
            subtitle = subtitle(),
            items = items,
            selected = selected,
            lines = if (helpOpen) Copy.HELP_LINES else emptyList(),
            hint = if (phase == SessionPhase.TITLE && !helpOpen) titleHint(selected) else "",
        )
    }

    fun onConfirm() {
        if (isGameOverConfirmBlocked) return
        if (phase == SessionPhase.PLAYING && !helpOpen) {
            flap()
            return
        }
        val items = itemsFor()
        if (items.isEmpty()) return
        val index = menuIndex.coerceIn(0, items.lastIndex)
        menuIndex = index
        activate(items[index].action)
    }

    fun onUp() {
        val items = itemsFor()
        if (items.isEmpty()) return
        menuIndex = (menuIndex - 1).coerceIn(0, items.lastIndex)
    }

    fun onDown() {
        val items = itemsFor()
        if (items.isEmpty()) return
        menuIndex = (menuIndex + 1).coerceIn(0, items.lastIndex)
    }

    fun onBack(): BackResult {
        if (helpOpen) {
            helpOpen = false
            menuIndex = 0
            return BackResult.STAY
        }
        return when (phase) {
            SessionPhase.PLAYING -> {
                phase = SessionPhase.PAUSED
                menuIndex = 0
                BackResult.STAY
            }
            SessionPhase.PAUSED -> {
                phase = SessionPhase.PLAYING
                menuIndex = 0
                BackResult.STAY
            }
            SessionPhase.GAME_OVER -> {
                showTitle()
                BackResult.STAY
            }
            SessionPhase.TITLE -> BackResult.FINISH
        }
    }

    fun togglePause() {
        when (phase) {
            SessionPhase.PLAYING -> {
                phase = SessionPhase.PAUSED
                menuIndex = 0
            }
            SessionPhase.PAUSED -> {
                phase = SessionPhase.PLAYING
                menuIndex = 0
            }
            else -> Unit
        }
    }

    /** Chamado quando a activity sai de primeiro plano. Não retoma sozinho. */
    fun background() {
        if (phase == SessionPhase.PLAYING) {
            phase = SessionPhase.PAUSED
            menuIndex = 0
        }
    }

    /**
     * Avança a espera do fim de jogo. Um passo maior que o teto de um quadro
     * não abre a trava inteira: o relógio de frames já limita o tranco.
     */
    fun advanceGameOverClock(seconds: Float) {
        if (phase != SessionPhase.GAME_OVER || !seconds.isFinite() || seconds <= 0f) return
        if (gameOverElapsed >= GAME_OVER_CONFIRM_LOCK_SECONDS) return
        val capped = seconds.coerceAtMost(tuning.maxFrameSeconds)
        gameOverElapsed = (gameOverElapsed + capped).coerceAtMost(GAME_OVER_CONFIRM_LOCK_SECONDS)
    }

    fun fixedUpdate() {
        if (phase != SessionPhase.PLAYING) return
        val dt = tuning.fixedDt
        aliveTime += dt
        wingEnergy = (wingEnergy - dt * 1.4f).coerceAtLeast(0.18f)
        val gravityScale = if (aliveTime < tuning.graceSeconds) tuning.graceGravityScale else 1f
        eyeVy = (eyeVy + tuning.gravity * gravityScale * dt).coerceAtMost(tuning.maxFallSpeed)
        eyeY += eyeVy * dt
        val minY = tuning.eyeRadius + tuning.ceilingPadding
        if (eyeY < minY) {
            eyeY = minY
            if (eyeVy < 0f) eyeVy = 0f
        }
        scroll(dt)
        val hit = eyeHitsGround(eyeY, tuning.hitRadius, tuning.groundY) ||
            gates.any { eyeHitsGate(tuning.eyeX, eyeY, tuning.hitRadius, it, tuning) }
        if (hit) {
            die()
            return
        }
        awardPassedGates()
        gates.removeAll { it.x + it.width < -tuning.despawnMargin }
    }

    fun capture(): SimulationState {
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
            bestEasy = best.get(Difficulty.EASY),
            bestNormal = best.get(Difficulty.NORMAL),
            gates = gates.toList(),
            gameOverElapsed = gameOverElapsed,
        )
    }

    fun restore(state: SimulationState) {
        phase = state.phase
        helpOpen = state.helpOpen
        difficulty = state.difficulty
        score = state.score.coerceAtLeast(0)
        newRecord = state.newRecord
        eyeY = state.eyeY
        eyeVy = state.eyeVy
        wingEnergy = state.wingEnergy
        aliveTime = state.aliveTime
        menuIndex = state.menuIndex.coerceAtLeast(0)
        nextSpawnX = state.nextSpawnX
        gates.clear()
        gates.addAll(state.gates)
        best.replace(state.bestEasy, state.bestNormal)
        gameOverElapsed = if (state.gameOverElapsed.isFinite()) {
            state.gameOverElapsed.coerceIn(0f, GAME_OVER_CONFIRM_LOCK_SECONDS)
        } else {
            0f
        }
    }

    fun toFrame(): RenderFrame {
        return RenderFrame(
            phase = phase,
            helpOpen = helpOpen,
            difficulty = difficulty,
            score = score,
            best = best.get(difficulty),
            newRecord = newRecord,
            eyeX = tuning.eyeX,
            eyeY = eyeY,
            eyeVy = eyeVy,
            eyeRadius = tuning.eyeRadius,
            wingEnergy = wingEnergy,
            aliveTime = aliveTime,
            pupilLook = (eyeVy / tuning.maxFallSpeed).coerceIn(-1f, 1f),
            gates = gates.toList(),
            menu = menu(),
            groundY = tuning.groundY,
            worldWidth = tuning.worldWidth,
            worldHeight = tuning.worldHeight,
        )
    }

    fun accessibilitySummary(): String {
        val selected = menu().items.getOrNull(menu().selected)?.label
        return when {
            helpOpen -> Copy.HELP_TITLE + ". " + Copy.HELP_LINES.joinToString(" ")
            phase == SessionPhase.TITLE -> buildString {
                append(Copy.TITLE)
                append(". ")
                append(Copy.PROVISIONAL)
                append(". ")
                if (selected != null) {
                    append(selected)
                    append(". ")
                }
                append(Copy.DISCLAIMER)
            }
            phase == SessionPhase.PLAYING ->
                "${Copy.POINTS} $score. ${Copy.BEST} ${best.get(difficulty)}."
            phase == SessionPhase.PAUSED -> "${Copy.PAUSED}. ${selected.orEmpty()}"
            else ->
                "${Copy.GAME_OVER}. ${Copy.POINTS} $score. ${selected.orEmpty()}"
        }
    }

    private fun flap() {
        if (phase != SessionPhase.PLAYING || helpOpen) return
        eyeVy = tuning.flapVelocity
        wingEnergy = 1f
    }

    private fun activate(action: MenuAction) {
        when (action) {
            MenuAction.START_EASY -> begin(Difficulty.EASY)
            MenuAction.START_NORMAL -> begin(Difficulty.NORMAL)
            MenuAction.OPEN_HELP -> {
                helpOpen = true
                menuIndex = 0
            }
            MenuAction.CLOSE_HELP -> {
                helpOpen = false
                menuIndex = 0
            }
            MenuAction.RESUME -> {
                if (phase == SessionPhase.PAUSED) {
                    phase = SessionPhase.PLAYING
                    menuIndex = 0
                }
            }
            MenuAction.RETRY -> begin(difficulty)
            MenuAction.TO_TITLE -> showTitle()
        }
    }

    private fun begin(mode: Difficulty) {
        difficulty = mode
        phase = SessionPhase.PLAYING
        helpOpen = false
        score = 0
        newRecord = false
        eyeY = tuning.eyeStartY
        eyeVy = 0f
        wingEnergy = 0.35f
        aliveTime = 0f
        menuIndex = 0
        gameOverElapsed = 0f
        resetGates()
    }

    private fun showTitle() {
        phase = SessionPhase.TITLE
        helpOpen = false
        score = 0
        newRecord = false
        eyeY = tuning.eyeStartY
        eyeVy = 0f
        wingEnergy = 0.25f
        aliveTime = 0f
        menuIndex = 0
        gameOverElapsed = 0f
        gates.clear()
        nextSpawnX = tuning.worldWidth + tuning.initialLead
    }

    private fun die() {
        if (phase != SessionPhase.PLAYING) return
        phase = SessionPhase.GAME_OVER
        menuIndex = 0
        val previous = best.get(difficulty)
        newRecord = score > previous && score > 0
        best.submit(difficulty, score)
        eyeVy = 0f
        gameOverElapsed = 0f
    }

    private fun resetGates() {
        gates.clear()
        var x = tuning.worldWidth + tuning.initialLead
        val spacing = tuning.spec(difficulty).spacing.coerceAtLeast(1f)
        repeat(tuning.initialGateCount) {
            gates += makeGate(x)
            x += spacing
        }
        nextSpawnX = x
    }

    private fun makeGate(x: Float): CloudGate {
        val gap = gapFor(tuning.spec(difficulty), score)
        return CloudGate(
            x = x,
            gapCenterY = gaps.next(gap, tuning),
            gapHeight = gap,
            width = tuning.gateWidth,
            passed = false,
        )
    }

    private fun scroll(dt: Float) {
        val dx = scrollSpeed() * dt
        for (index in gates.indices) {
            val gate = gates[index]
            gates[index] = gate.copy(x = gate.x - dx)
        }
        nextSpawnX -= dx
        val spacing = tuning.spec(difficulty).spacing.coerceAtLeast(1f)
        while (nextSpawnX < tuning.worldWidth + tuning.spawnAhead && gates.size < MAX_GATES) {
            gates += makeGate(nextSpawnX)
            nextSpawnX += spacing
        }
    }

    private fun awardPassedGates() {
        for (index in gates.indices) {
            val gate = gates[index]
            if (!gate.passed && gate.x + gate.width < tuning.eyeX) {
                gates[index] = gate.copy(passed = true)
                score += 1
            }
        }
    }

    private fun itemsFor(): List<MenuItem> = when {
        helpOpen -> listOf(MenuItem(Copy.BACK, MenuAction.CLOSE_HELP))
        phase == SessionPhase.TITLE -> listOf(
            MenuItem(Copy.EASY, MenuAction.START_EASY),
            MenuItem(Copy.NORMAL, MenuAction.START_NORMAL),
            MenuItem(Copy.HOW_TO, MenuAction.OPEN_HELP),
        )
        phase == SessionPhase.PAUSED -> listOf(
            MenuItem(Copy.CONTINUE, MenuAction.RESUME),
            MenuItem(Copy.RETRY, MenuAction.RETRY),
            MenuItem(Copy.TO_TITLE, MenuAction.TO_TITLE),
        )
        phase == SessionPhase.GAME_OVER -> listOf(
            MenuItem(Copy.RETRY, MenuAction.RETRY),
            MenuItem(Copy.TO_TITLE, MenuAction.TO_TITLE),
        )
        else -> emptyList()
    }

    private fun heading(): String = when {
        helpOpen -> Copy.HELP_TITLE
        phase == SessionPhase.PAUSED -> Copy.PAUSED
        phase == SessionPhase.GAME_OVER -> Copy.GAME_OVER
        else -> Copy.TITLE
    }

    private fun subtitle(): String = when {
        helpOpen -> ""
        phase == SessionPhase.PAUSED -> Copy.PAUSED_HINT
        phase == SessionPhase.GAME_OVER -> Copy.HIT_CLOUD
        else -> ""
    }

    private fun titleHint(index: Int): String = when (index) {
        0 -> Copy.EASY_HINT
        1 -> Copy.NORMAL_HINT
        else -> Copy.HOW_TO_HINT
    }

    companion object {
        const val GAME_OVER_CONFIRM_LOCK_SECONDS = 0.7f
        private const val MAX_GATES = 8
    }
}
