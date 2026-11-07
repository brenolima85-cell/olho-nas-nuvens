package app.olhonasnuvens.tv.render

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import app.olhonasnuvens.tv.game.CloudGate
import app.olhonasnuvens.tv.game.Copy
import app.olhonasnuvens.tv.game.Difficulty
import app.olhonasnuvens.tv.game.MenuState
import app.olhonasnuvens.tv.game.Motion
import app.olhonasnuvens.tv.game.RenderFrame
import app.olhonasnuvens.tv.game.SessionPhase
import kotlin.math.sin

/**
 * Desenho original do céu, dos bancos de nuvem e do olho com asinhas.
 * O quadro lógico é 16:9 e é escalado para caber na TV.
 */
class SceneRenderer {
    private val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunPaint = fill(Palette.sun)
    private val sunGlowPaint = fill(Palette.sunGlow)
    private val farCloudPaint = fill(Palette.farCloud)
    private val cloudFill = fill(Palette.cloud)
    private val cloudShade = fill(Palette.cloudShade)
    private val cloudHighlight = fill(Palette.cloudHighlight)
    private val hillPaint = fill(Palette.hill)
    private val hillDeepPaint = fill(Palette.hillDeep)
    private val hillLinePaint = stroke(Palette.hillLine, 8f)
    private val wingFill = fill(Palette.wing)
    private val wingStroke = stroke(Palette.outline, 4f)
    private val scleraPaint = fill(Palette.sclera)
    private val outlinePaint = stroke(Palette.outline, 6f)
    private val irisPaint = fill(Palette.iris)
    private val irisInnerPaint = fill(Palette.irisInner)
    private val pupilPaint = fill(Palette.pupil)
    private val highlightPaint = fill(Palette.cloudHighlight)
    private val lidPaint = fill(Palette.lid)
    private val blushPaint = fill(Palette.blush)
    private val shadowPaint = fill(Palette.shadow)
    private val scrimPaint = fill(Palette.scrim)
    private val panelPaint = fill(Palette.panel)
    private val pillPaint = fill(Palette.pill)
    private val buttonFill = fill(Palette.button)
    private val buttonStroke = stroke(Palette.buttonEdge, 3f)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        isSubpixelText = true
    }
    private val path = Path()
    private val clipPath = Path()
    private val rect = RectF()
    private var skyShader: LinearGradient? = null
    private var shaderHeight = 0f

    fun draw(canvas: Canvas, viewWidth: Int, viewHeight: Int, frame: RenderFrame, cosmeticTime: Float) {
        canvas.drawColor(Palette.skyBottom)
        if (viewWidth <= 0 || viewHeight <= 0 || frame.worldWidth <= 0f || frame.worldHeight <= 0f) return
        val scale = minOf(viewWidth / frame.worldWidth, viewHeight / frame.worldHeight)
        val dx = (viewWidth - frame.worldWidth * scale) / 2f
        val dy = (viewHeight - frame.worldHeight * scale) / 2f
        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        drawWorld(canvas, frame, cosmeticTime)
        canvas.restore()
    }

    private fun drawWorld(canvas: Canvas, frame: RenderFrame, cosmeticTime: Float) {
        drawSky(canvas, frame)
        drawSun(canvas, frame)
        drawFarClouds(canvas, frame, cosmeticTime)
        if (frame.phase == SessionPhase.TITLE) {
            drawGround(canvas, frame)
            val bob = sin(cosmeticTime * 1.6f) * 14f
            drawEye(
                canvas,
                frame.worldWidth * 0.24f,
                frame.worldHeight * 0.56f + bob,
                frame.worldHeight * 0.115f,
                0.45f,
                cosmeticTime,
                0f,
            )
            drawTitle(canvas, frame)
        } else {
            frame.gates.forEach { drawGate(canvas, it, frame.groundY) }
            drawGround(canvas, frame)
            drawEye(
                canvas,
                frame.eyeX,
                frame.eyeY,
                frame.eyeRadius,
                frame.wingEnergy,
                frame.aliveTime,
                frame.pupilLook,
            )
            drawHud(canvas, frame)
        }
        if (frame.helpOpen || frame.phase == SessionPhase.PAUSED || frame.phase == SessionPhase.GAME_OVER) {
            drawOverlay(canvas, frame)
        }
    }

    private fun drawSky(canvas: Canvas, frame: RenderFrame) {
        if (shaderHeight != frame.worldHeight) {
            skyShader = LinearGradient(
                0f,
                0f,
                0f,
                frame.worldHeight,
                intArrayOf(Palette.skyTop, Palette.skyMid, Palette.skyBottom),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP,
            )
            skyPaint.shader = skyShader
            shaderHeight = frame.worldHeight
        }
        canvas.drawRect(0f, 0f, frame.worldWidth, frame.worldHeight, skyPaint)
    }

    private fun drawSun(canvas: Canvas, frame: RenderFrame) {
        val cx = frame.worldWidth * 0.82f
        val cy = frame.worldHeight * 0.16f
        canvas.drawCircle(cx, cy, 118f, sunGlowPaint)
        canvas.drawCircle(cx, cy, 62f, sunPaint)
    }

    private fun drawFarClouds(canvas: Canvas, frame: RenderFrame, cosmeticTime: Float) {
        val drift = if (frame.phase == SessionPhase.TITLE && !frame.helpOpen) cosmeticTime * 16f else 0f
        val span = frame.worldWidth + 240f
        for (puff in FAR_CLOUDS) {
            val x = ((puff.x + drift) % span + span) % span - 120f
            canvas.drawCircle(x, puff.y, puff.r, farCloudPaint)
            canvas.drawCircle(x + puff.r * 0.7f, puff.y + 8f, puff.r * 0.72f, farCloudPaint)
        }
    }

    private fun drawGround(canvas: Canvas, frame: RenderFrame) {
        val y = frame.groundY
        canvas.drawRect(0f, y, frame.worldWidth, frame.worldHeight, hillPaint)
        canvas.drawRect(0f, y + 36f, frame.worldWidth, frame.worldHeight, hillDeepPaint)
        canvas.save()
        canvas.clipRect(0f, y, frame.worldWidth, frame.worldHeight)
        var x = 20f
        while (x < frame.worldWidth + 40f) {
            canvas.drawCircle(x, y + 48f, 48f, hillPaint)
            x += 110f
        }
        canvas.restore()
        canvas.drawLine(0f, y, frame.worldWidth, y, hillLinePaint)
    }

    private fun drawGate(canvas: Canvas, gate: CloudGate, groundY: Float) {
        val visualTop = gate.gapCenterY - gate.gapHeight / 2f
        val visualBottom = gate.gapCenterY + gate.gapHeight / 2f
        drawBank(canvas, gate.x, gate.x + gate.width, -48f, visualTop, lipAtBottom = true)
        drawBank(canvas, gate.x, gate.x + gate.width, visualBottom, groundY, lipAtBottom = false)
    }

    private fun drawBank(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        lipAtBottom: Boolean,
    ) {
        if (bottom - top < 16f || right - left < 16f) return
        canvas.save()
        canvas.clipRect(left - 8f, top, right + 8f, bottom)
        val mid = (left + right) / 2f
        var y = top + 8f
        var index = 0
        while (y < bottom + 24f) {
            val wobble = if (index % 2 == 0) -16f else 18f
            canvas.drawCircle(mid + wobble, y, 72f, cloudShade)
            canvas.drawCircle(mid - 40f + wobble, y + 8f, 56f, cloudFill)
            canvas.drawCircle(mid + 38f + wobble, y + 6f, 52f, cloudFill)
            canvas.drawCircle(mid + wobble * 0.3f, y - 10f, 34f, cloudHighlight)
            y += 56f
            index++
        }
        canvas.restore()
        val lipY = if (lipAtBottom) bottom else top
        canvas.save()
        val scallop = 18f
        if (lipAtBottom) {
            canvas.clipRect(left - 4f, lipY, right + 4f, lipY + scallop)
        } else {
            canvas.clipRect(left - 4f, lipY - scallop, right + 4f, lipY)
        }
        var x = left + 8f
        while (x < right) {
            canvas.drawCircle(x, lipY, 26f, cloudFill)
            x += 34f
        }
        canvas.restore()
    }

    private fun drawEye(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        energy: Float,
        time: Float,
        look: Float,
    ) {
        canvas.drawOval(
            cx - radius * 0.95f,
            cy + radius * 0.78f,
            cx + radius * 0.95f,
            cy + radius * 1.12f,
            shadowPaint,
        )
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(radius / BASE_RADIUS, radius / BASE_RADIUS)
        drawEyeLocal(canvas, energy, time, look)
        canvas.restore()
    }

    private fun drawEyeLocal(canvas: Canvas, energy: Float, time: Float, look: Float) {
        val radius = BASE_RADIUS
        val flap = sin(time * 6.2f) * (10f + 18f * energy.coerceIn(0f, 1f))
        drawWing(canvas, -radius * 0.2f, 6f, -1f, 20f + flap)
        drawWing(canvas, radius * 0.2f, 6f, 1f, 20f + flap)
        canvas.drawCircle(0f, 0f, radius, scleraPaint)
        canvas.drawCircle(0f, look * 6f, 31f, irisPaint)
        canvas.drawCircle(0f, look * 8f, 18f, irisInnerPaint)
        canvas.drawCircle(look * 2f, look * 10f, 11f, pupilPaint)
        canvas.drawCircle(-15f, -16f + look * 4f, 8f, highlightPaint)
        canvas.drawCircle(9f, -6f + look * 3f, 4f, highlightPaint)
        canvas.drawCircle(-28f, 22f, 8f, blushPaint)
        canvas.drawCircle(28f, 22f, 8f, blushPaint)
        val open = Motion.eyelidOpen(time)
        val cover = radius * (1f - open)
        val rim = radius * 0.16f
        clipPath.reset()
        clipPath.addCircle(0f, 0f, radius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawRect(-radius - 6f, -radius - 8f, radius + 6f, -radius + rim + cover, lidPaint)
        canvas.drawRect(-radius - 6f, radius - rim - cover, radius + 6f, radius + 8f, lidPaint)
        canvas.restore()
        outlinePaint.strokeWidth = 6f
        canvas.drawCircle(0f, 0f, radius, outlinePaint)
    }

    private fun drawWing(canvas: Canvas, pivotX: Float, pivotY: Float, direction: Float, angle: Float) {
        canvas.save()
        canvas.translate(pivotX, pivotY)
        canvas.rotate(direction * angle)
        path.reset()
        path.moveTo(0f, 0f)
        path.quadTo(direction * 28f, -30f, direction * 74f, -4f)
        path.quadTo(direction * 48f, 10f, direction * 66f, 18f)
        path.quadTo(direction * 24f, 14f, 0f, 8f)
        path.close()
        canvas.drawPath(path, wingFill)
        wingStroke.strokeWidth = 4f
        canvas.drawPath(path, wingStroke)
        canvas.restore()
    }

    private fun drawTitle(canvas: Canvas, frame: RenderFrame) {
        val center = frame.worldWidth / 2f
        drawCreamLabel(
            canvas,
            center,
            frame.worldHeight * 0.11f,
            Copy.PROVISIONAL,
            32f,
            frame.worldWidth * 0.86f,
        )
        drawCreamLabel(
            canvas,
            center,
            frame.worldHeight * 0.21f,
            Copy.TITLE,
            72f,
            frame.worldWidth * 0.8f,
        )
        drawCreamLabel(
            canvas,
            center,
            frame.worldHeight * 0.30f,
            Copy.SUBTITLE,
            32f,
            frame.worldWidth * 0.7f,
        )
        val menu = frame.menu
        val buttonW = frame.worldWidth * 0.32f
        val buttonX = frame.worldWidth * 0.60f
        var y = frame.worldHeight * 0.40f
        menu.items.forEachIndexed { index, item ->
            drawButton(canvas, buttonX, y, buttonW, BUTTON_H, item.label, index == menu.selected)
            y += BUTTON_H + BUTTON_GAP
        }
        if (menu.hint.isNotEmpty()) {
            drawCreamLabel(canvas, buttonX + buttonW / 2f, y + 28f, menu.hint, 32f, buttonW + 120f)
        }
        drawCreamLabel(
            canvas,
            center,
            frame.groundY - 56f,
            Copy.DISCLAIMER,
            32f,
            frame.worldWidth * 0.92f,
        )
    }

    private fun drawHud(canvas: Canvas, frame: RenderFrame) {
        val mode = if (frame.difficulty == Difficulty.EASY) Copy.EASY else Copy.NORMAL
        drawCreamLabel(canvas, frame.worldWidth * 0.16f, frame.worldHeight * 0.12f, mode, 32f, 420f)
        drawCreamLabel(
            canvas,
            frame.worldWidth / 2f,
            frame.worldHeight * 0.125f,
            "${Copy.POINTS} ${frame.score}",
            54f,
            520f,
        )
        drawCreamLabel(
            canvas,
            frame.worldWidth * 0.84f,
            frame.worldHeight * 0.12f,
            "${Copy.BEST} ${frame.best}",
            32f,
            460f,
        )
    }

    private fun drawOverlay(canvas: Canvas, frame: RenderFrame) {
        canvas.drawRect(0f, 0f, frame.worldWidth, frame.worldHeight, scrimPaint)
        val menu = frame.menu
        val panelW = frame.worldWidth * 0.82f
        val panelH = overlayHeight(frame, menu).coerceAtMost(frame.worldHeight - 48f)
        val left = (frame.worldWidth - panelW) / 2f
        val top = ((frame.worldHeight - panelH) / 2f).coerceAtLeast(24f)
        rect.set(left, top, left + panelW, top + panelH)
        canvas.drawRoundRect(rect, 36f, 36f, panelPaint)
        var y = top + PAD
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textPaint.color = Palette.text
        textPaint.textSize = 58f
        canvas.drawText(menu.heading, frame.worldWidth / 2f, y + 52f, textPaint)
        y += HEADING_BLOCK
        if (menu.subtitle.isNotEmpty()) {
            textPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textPaint.color = Palette.muted
            textPaint.textSize = 34f
            canvas.drawText(menu.subtitle, frame.worldWidth / 2f, y + 28f, textPaint)
            y += SUBTITLE_BLOCK
        }
        if (frame.phase == SessionPhase.GAME_OVER && !frame.helpOpen) {
            textPaint.textSize = 32f
            textPaint.color = Palette.muted
            canvas.drawText(Copy.POINTS, frame.worldWidth / 2f, y + 28f, textPaint)
            textPaint.textSize = 72f
            textPaint.color = Palette.text
            textPaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            canvas.drawText(frame.score.toString(), frame.worldWidth / 2f, y + 104f, textPaint)
            textPaint.textSize = 34f
            textPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textPaint.color = if (frame.newRecord) Palette.record else Palette.muted
            val bestLine = if (frame.newRecord) {
                Copy.NEW_RECORD
            } else {
                "${Copy.bestLabel(frame.difficulty)}: ${frame.best}"
            }
            canvas.drawText(bestLine, frame.worldWidth / 2f, y + 154f, textPaint)
            y += SCORE_BLOCK
        }
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textPaint.color = Palette.text
        textPaint.textSize = 34f
        for (line in menu.lines) {
            canvas.drawText(line, left + PAD, y + 32f, textPaint)
            y += LINE
        }
        val buttonW = panelW - PAD * 2f
        menu.items.forEachIndexed { index, item ->
            drawButton(canvas, left + PAD, y, buttonW, BUTTON_H, item.label, index == menu.selected)
            y += BUTTON_H + BUTTON_GAP
        }
    }

    private fun overlayHeight(frame: RenderFrame, menu: MenuState): Float {
        var height = PAD + HEADING_BLOCK + PAD
        if (menu.subtitle.isNotEmpty()) height += SUBTITLE_BLOCK
        if (frame.phase == SessionPhase.GAME_OVER && !frame.helpOpen) height += SCORE_BLOCK
        height += menu.lines.size * LINE
        height += menu.items.size * (BUTTON_H + BUTTON_GAP)
        return height
    }

    private fun drawButton(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        focused: Boolean,
    ) {
        rect.set(x, y, x + width, y + height)
        buttonFill.color = if (focused) Palette.focusFill else Palette.button
        canvas.drawRoundRect(rect, 28f, 28f, buttonFill)
        buttonStroke.color = if (focused) Palette.focusStroke else Palette.buttonEdge
        buttonStroke.strokeWidth = if (focused) 7f else 3f
        canvas.drawRoundRect(rect, 28f, 28f, buttonStroke)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textPaint.color = Palette.text
        textPaint.textSize = 40f
        val shown = if (focused) "▶  $label" else "    $label"
        val textWidth = textPaint.measureText(shown)
        val baseline = y + height / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(shown, x + (width - textWidth) / 2f, baseline, textPaint)
    }

    private fun drawCreamLabel(
        canvas: Canvas,
        cx: Float,
        baseline: Float,
        label: String,
        textSize: Float,
        maxWidth: Float,
    ) {
        textPaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Palette.navy
        var size = textSize
        textPaint.textSize = size
        while (textPaint.measureText(label) > maxWidth && size > 18f) {
            size -= 1f
            textPaint.textSize = size
        }
        val textWidth = textPaint.measureText(label)
        val padX = 28f
        val top = baseline + textPaint.ascent() - 14f
        val bottom = baseline + textPaint.descent() + 14f
        rect.set(cx - textWidth / 2f - padX, top, cx + textWidth / 2f + padX, bottom)
        canvas.drawRoundRect(rect, 22f, 22f, pillPaint)
        canvas.drawText(label, cx - textWidth / 2f, baseline, textPaint)
    }

    private fun fill(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    private fun stroke(color: Int, width: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = width
        this.color = color
    }

    private companion object {
        const val BASE_RADIUS = 58f
        const val PAD = 48f
        const val HEADING_BLOCK = 78f
        const val SUBTITLE_BLOCK = 52f
        const val LINE = 54f
        const val SCORE_BLOCK = 188f
        const val BUTTON_H = 96f
        const val BUTTON_GAP = 18f

        data class FarCloud(val x: Float, val y: Float, val r: Float)

        val FAR_CLOUDS = listOf(
            FarCloud(260f, 170f, 42f),
            FarCloud(980f, 140f, 36f),
            FarCloud(1460f, 200f, 48f),
        )
    }
}
