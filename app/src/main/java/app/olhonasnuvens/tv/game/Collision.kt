package app.olhonasnuvens.tv.game

data class Aabb(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

fun circleIntersectsAabb(
    cx: Float,
    cy: Float,
    radius: Float,
    box: Aabb,
): Boolean {
    if (box.right <= box.left || box.bottom <= box.top || radius < 0f) return false
    val nearestX = cx.coerceIn(box.left, box.right)
    val nearestY = cy.coerceIn(box.top, box.bottom)
    val dx = cx - nearestX
    val dy = cy - nearestY
    return dx * dx + dy * dy <= radius * radius
}

/**
 * Miolo sólido do banco de nuvem. O desenho pode ultrapassar essa caixa;
 * encostar só na borda fofa não conta como colisão.
 */
fun solidCores(gate: CloudGate, tuning: Tuning): List<Aabb> {
    val left = gate.x + tuning.sideForgiveness
    val right = gate.x + gate.width - tuning.sideForgiveness
    if (right <= left) return emptyList()
    val visualTop = gate.gapCenterY - gate.gapHeight / 2f
    val visualBottom = gate.gapCenterY + gate.gapHeight / 2f
    val topSolidBottom = visualTop - tuning.gapForgiveness
    val bottomSolidTop = visualBottom + tuning.gapForgiveness
    val cores = ArrayList<Aabb>(2)
    if (topSolidBottom > 0f) {
        cores += Aabb(left, 0f, right, topSolidBottom)
    }
    if (bottomSolidTop < tuning.worldHeight) {
        cores += Aabb(left, bottomSolidTop, right, tuning.worldHeight)
    }
    return cores
}

fun eyeHitsGate(
    eyeX: Float,
    eyeY: Float,
    hitRadius: Float,
    gate: CloudGate,
    tuning: Tuning,
): Boolean {
    return solidCores(gate, tuning).any { box ->
        circleIntersectsAabb(eyeX, eyeY, hitRadius, box)
    }
}

fun eyeHitsGround(eyeY: Float, hitRadius: Float, groundY: Float): Boolean {
    return eyeY + hitRadius >= groundY
}
