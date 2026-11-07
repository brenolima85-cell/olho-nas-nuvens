package app.olhonasnuvens.tv.game

/**
 * Quadro lógico 16:9. Valores de partida, ainda sem ajuste em TV real.
 * O eixo Y cresce para baixo: velocidade negativa sobe.
 */
data class Tuning(
    val worldWidth: Float = 1920f,
    val worldHeight: Float = 1080f,
    val fixedDt: Float = 1f / 60f,
    val maxFrameSeconds: Float = 0.25f,
    val maxStepsPerFrame: Int = 5,
    val eyeX: Float = 520f,
    val eyeStartY: Float = 470f,
    val eyeRadius: Float = 58f,
    val hitboxScale: Float = 0.62f,
    val groundY: Float = 980f,
    val ceilingPadding: Float = 16f,
    val gravity: Float = 1700f,
    val flapVelocity: Float = -860f,
    val maxFallSpeed: Float = 900f,
    val graceSeconds: Float = 1.1f,
    val graceGravityScale: Float = 0.35f,
    val gateWidth: Float = 228f,
    val sideForgiveness: Float = 46f,
    val gapForgiveness: Float = 42f,
    val initialLead: Float = 120f,
    val initialGateCount: Int = 3,
    val spawnAhead: Float = 40f,
    val despawnMargin: Float = 80f,
    val easy: ModeSpec = ModeSpec(
        baseSpeed = 168f,
        maxSpeed = 168f,
        speedStep = 0f,
        baseGap = 540f,
        minGap = 540f,
        gapStep = 0f,
        spacing = 860f,
        progressEvery = 0,
    ),
    val normal: ModeSpec = ModeSpec(
        baseSpeed = 230f,
        maxSpeed = 360f,
        speedStep = 14f,
        baseGap = 400f,
        minGap = 310f,
        gapStep = 10f,
        spacing = 700f,
        progressEvery = 2,
    ),
) {
    fun spec(difficulty: Difficulty): ModeSpec = when (difficulty) {
        Difficulty.EASY -> easy
        Difficulty.NORMAL -> normal
    }

    val hitRadius: Float get() = eyeRadius * hitboxScale
}

data class ModeSpec(
    val baseSpeed: Float,
    val maxSpeed: Float,
    val speedStep: Float,
    val baseGap: Float,
    val minGap: Float,
    val gapStep: Float,
    val spacing: Float,
    val progressEvery: Int,
)

fun speedFor(spec: ModeSpec, score: Int): Float {
    if (spec.progressEvery <= 0 || spec.speedStep == 0f) return spec.baseSpeed
    val levels = (score.coerceAtLeast(0)) / spec.progressEvery
    return (spec.baseSpeed + spec.speedStep * levels).coerceAtMost(spec.maxSpeed)
}

fun gapFor(spec: ModeSpec, score: Int): Float {
    if (spec.progressEvery <= 0 || spec.gapStep == 0f) return spec.baseGap
    val levels = (score.coerceAtLeast(0)) / spec.progressEvery
    return (spec.baseGap - spec.gapStep * levels).coerceAtLeast(spec.minGap)
}
