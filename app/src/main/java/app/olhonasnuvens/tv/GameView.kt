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
import app.olhonasnuvens.tv.game.SummaryAnnouncer
import app.olhonasnuvens.tv.input.InputDecoder
import app.olhonasnuvens.tv.input.InputOutcome
import app.olhonasnuvens.tv.input.SessionInput
import app.olhonasnuvens.tv.input.StickDirection
import app.olhonasnuvens.tv.input.StickNavigator
import app.olhonasnuvens.tv.input.TvKeyCodes
import app.olhonasnuvens.tv.input.classifyKey
import app.olhonasnuvens.tv.input.holdsGameOverConfirm
import app.olhonasnuvens.tv.input.isHorizontalKey
import app.olhonasnuvens.tv.render.Palette
import app.olhonasnuvens.tv.render.SceneRenderer

/**
 * Relógio do jogo. A física só avança em [SessionPhase.PLAYING].
 * No fim de jogo, o mesmo passo fixo libera o OK depois de uma espera curta.
 * Ao ir para segundo plano o relógio de frames zera, para o tempo fora da tela não contar.
 */
class GameView(
    context: Context,
    private val session: GameSession,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var onExitRequested: (() -> Unit)? = null

    private val renderer = SceneRenderer()
    private val announcer = SummaryAnnouncer()
    private val decoder = InputDecoder()
    private val stick = StickNavigator()
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
        releaseDirectionalHold()
        session.background()
        invalidate()
    }

    /**
     * Passo de menu vindo do analógico ou do hat. Não sobe o olho.
     * Segurar o eixo não entra aqui de novo: o [StickNavigator] só entrega a borda.
     */
    fun handleDirectionalAction(isUp: Boolean, eventTimeMillis: Long) {
        val keyCode = if (isUp) TvKeyCodes.DPAD_UP else TvKeyCodes.DPAD_DOWN
        val playing = session.phase == SessionPhase.PLAYING && !session.helpOpen
        val whenMs = if (eventTimeMillis > 0L) eventTimeMillis else SystemClock.uptimeMillis()
        applyOutcome(decoder.decode(keyCode, 0, whenMs, playing))
    }

    fun sampleStick(y: Float, hatY: Float, eventTimeMillis: Long) {
        when (stick.onSample(y, hatY)) {
            StickDirection.UP -> handleDirectionalAction(isUp = true, eventTimeMillis)
            StickDirection.DOWN -> handleDirectionalAction(isUp = false, eventTimeMillis)
            null -> Unit
        }
    }

    fun releaseDirectionalHold() {
        stick.disarm()
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val playing = session.phase == SessionPhase.PLAYING && !session.helpOpen
        val whenMs = if (event.eventTime > 0L) event.eventTime else SystemClock.uptimeMillis()
        if (event.action == KeyEvent.ACTION_UP) {
            return classifyKey(event.keyCode) != null || isHorizontalKey(event.keyCode)
        }
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (holdsGameOverConfirm(session, event.keyCode)) return true
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
        announcer.update(session.accessibilitySummary())?.let { contentDescription = it }
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
            when (session.phase) {
                SessionPhase.PLAYING -> session.fixedUpdate()
                SessionPhase.TITLE -> if (!session.helpOpen) {
                    cosmeticTime += dt
                    if (cosmeticTime > COSMETIC_WRAP) cosmeticTime -= COSMETIC_WRAP
                }
                SessionPhase.GAME_OVER -> session.advanceGameOverClock(dt)
                SessionPhase.PAUSED -> Unit
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
