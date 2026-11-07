package app.olhonasnuvens.tv

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Choreographer
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import app.olhonasnuvens.tv.game.FixedStepper
import app.olhonasnuvens.tv.game.GameSession
import app.olhonasnuvens.tv.game.SessionPhase
import app.olhonasnuvens.tv.input.InputDecoder
import app.olhonasnuvens.tv.input.InputOutcome
import app.olhonasnuvens.tv.input.SessionInput
import app.olhonasnuvens.tv.input.TvKeyCodes
import app.olhonasnuvens.tv.input.classifyKey
import app.olhonasnuvens.tv.input.isHorizontalKey
import app.olhonasnuvens.tv.render.Palette
import app.olhonasnuvens.tv.render.SceneRenderer

/**
 * Relógio do jogo. A física só avança em [SessionPhase.PLAYING].
 * Ao ir para segundo plano o relógio zera, para o tempo fora da tela não virar um salto.
 */
class GameView(
    context: Context,
    private val session: GameSession,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var onExitRequested: (() -> Unit)? = null

    private val renderer = SceneRenderer()
    private val decoder = InputDecoder()
    private val stepper = FixedStepper(
        fixedDt = session.tuning.fixedDt,
        maxFrameSeconds = session.tuning.maxFrameSeconds,
        maxSteps = session.tuning.maxStepsPerFrame,
    )
    private val choreographer = Choreographer.getInstance()
    private val frameCallback = Choreographer.FrameCallback { frameNs -> onFrame(frameNs) }
    private var hostResumed = false
    private var loopPosted = false
    private var cosmeticTime = 0f

    init {
        isFocusable = true
        isFocusableInTouchMode = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        setBackgroundColor(Palette.skyBottom)
    }

    fun onHostResume() {
        hostResumed = true
        stepper.reset()
        ensureLoop()
        requestFocus()
        invalidate()
    }

    fun onHostPause() {
        hostResumed = false
        loopPosted = false
        choreographer.removeFrameCallback(frameCallback)
        stepper.reset()
        session.background()
        invalidate()
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val playing = session.phase == SessionPhase.PLAYING && !session.helpOpen
        val whenMs = if (event.eventTime > 0L) event.eventTime else SystemClock.uptimeMillis()
        if (event.action == KeyEvent.ACTION_UP) {
            return classifyKey(event.keyCode) != null || isHorizontalKey(event.keyCode)
        }
        if (event.action != KeyEvent.ACTION_DOWN) return false
        return applyOutcome(decoder.decode(event.keyCode, event.repeatCount, whenMs, playing))
    }

    /**
     * Caminho de Voltar quando a plataforma não entrega KEYCODE_BACK.
     * O mesmo debounce evita pausar e retomar no mesmo aperto.
     */
    fun handleBackFallback(): Boolean {
        val playing = session.phase == SessionPhase.PLAYING && !session.helpOpen
        val outcome = decoder.decode(TvKeyCodes.BACK, 0, SystemClock.uptimeMillis(), playing)
        return applyOutcome(outcome)
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val summary = session.accessibilitySummary()
        if (contentDescription != summary) contentDescription = summary
        renderer.draw(canvas, width, height, session.toFrame(), cosmeticTime)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = false

    private fun applyOutcome(outcome: InputOutcome): Boolean {
        return when (outcome) {
            InputOutcome.Unhandled -> false
            InputOutcome.Consumed -> true
            is InputOutcome.Action -> {
                val finish = SessionInput.apply(outcome.action, session)
                if (finish) onExitRequested?.invoke()
                ensureLoop()
                invalidate()
                true
            }
        }
    }

    private fun ensureLoop() {
        if (!hostResumed || loopPosted) return
        loopPosted = true
        choreographer.postFrameCallback(frameCallback)
    }

    private fun onFrame(frameNs: Long) {
        if (!hostResumed) {
            loopPosted = false
            return
        }
        val steps = stepper.advanceNanos(frameNs)
        val dt = session.tuning.fixedDt
        repeat(steps) {
            when {
                session.phase == SessionPhase.PLAYING -> session.fixedUpdate()
                session.phase == SessionPhase.TITLE && !session.helpOpen -> {
                    cosmeticTime += dt
                    if (cosmeticTime > COSMETIC_WRAP) cosmeticTime -= COSMETIC_WRAP
                }
            }
        }
        invalidate()
        if (hostResumed) {
            choreographer.postFrameCallback(frameCallback)
        } else {
            loopPosted = false
        }
    }

    private companion object {
        const val COSMETIC_WRAP = 10_000f
    }
}
