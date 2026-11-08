package app.olhonasnuvens.tv

import android.app.Activity
import android.content.Context
import android.hardware.input.InputManager
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import app.olhonasnuvens.tv.game.GameSession
import app.olhonasnuvens.tv.game.RandomGapSource
import app.olhonasnuvens.tv.game.StateCodec
import app.olhonasnuvens.tv.input.ConnectedControllers
import app.olhonasnuvens.tv.input.MotionActions
import app.olhonasnuvens.tv.input.PlayControllerSources
import app.olhonasnuvens.tv.persist.LocalBestScores
import kotlin.random.Random

class MainActivity : Activity() {
    private lateinit var session: GameSession
    private lateinit var gameView: GameView
    private val controllers = ConnectedControllers()
    private var watchingControllers = false

    private val inputManager: InputManager? by lazy {
        getSystemService(Context.INPUT_SERVICE) as? InputManager
    }

    private val deviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
            noteDevice(deviceId)
        }

        override fun onInputDeviceChanged(deviceId: Int) {
            val device = inputManager?.getInputDevice(deviceId)
            if (device == null) {
                if (controllers.remove(deviceId)) onPlayControllerLost()
                return
            }
            if (controllers.update(deviceId, device.sources).lostPlayController) {
                onPlayControllerLost()
            }
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            if (controllers.remove(deviceId)) onPlayControllerLost()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersive()
        session = GameSession(
            gaps = RandomGapSource(Random(System.currentTimeMillis())),
            best = LocalBestScores.load(this),
        )
        savedInstanceState?.getString(STATE_KEY)?.let { raw ->
            StateCodec.decode(raw)?.let { session.restore(it) }
        }
        session.background()
        gameView = GameView(this, session)
        gameView.onExitRequested = { finish() }
        setContentView(gameView)
    }

    override fun onResume() {
        super.onResume()
        applyImmersive()
        startControllerWatch()
        gameView.onHostResume()
    }

    override fun onPause() {
        stopControllerWatch()
        if (::gameView.isInitialized) gameView.onHostPause()
        if (::session.isInitialized) LocalBestScores.save(this, session.best)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::session.isInitialized) {
            outState.putString(STATE_KEY, StateCodec.encode(session.capture()))
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersive()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::gameView.isInitialized && gameView.handleKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (!::gameView.isInitialized || !PlayControllerSources.isStickSource(event.source)) {
            return super.onGenericMotionEvent(event)
        }
        val whenMs = if (event.eventTime > 0L) event.eventTime else SystemClock.uptimeMillis()
        when {
            MotionActions.samplesAxes(event.actionMasked) -> gameView.sampleStick(
                y = event.getAxisValue(MotionEvent.AXIS_Y),
                hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y),
                eventTimeMillis = whenMs,
            )
            MotionActions.releasesHold(event.actionMasked) -> gameView.releaseDirectionalHold()
        }
        return true
    }

    /**
     * Voltar no framework, sem AndroidX. O manifesto desliga o callback preditivo
     * (`enableOnBackInvokedCallback=false`) para o remoto antigo continuar entregando
     * KEYCODE_BACK. Se a plataforma só chamar este método, o mesmo debounce do
     * [GameView.handleBackFallback] evita pausar e retomar no mesmo aperto.
     */
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::gameView.isInitialized && gameView.handleBackFallback()) return
        super.onBackPressed()
    }

    private fun noteDevice(deviceId: Int) {
        val sources = inputManager?.getInputDevice(deviceId)?.sources ?: return
        controllers.update(deviceId, sources)
    }

    private fun onPlayControllerLost() {
        if (::gameView.isInitialized) gameView.releaseDirectionalHold()
        if (::session.isInitialized) session.background()
    }

    private fun startControllerWatch() {
        val manager = inputManager ?: return
        if (watchingControllers) return
        manager.inputDeviceIds.forEach { noteDevice(it) }
        manager.registerInputDeviceListener(deviceListener, null)
        watchingControllers = true
    }

    private fun stopControllerWatch() {
        val manager = inputManager ?: return
        if (!watchingControllers) return
        manager.unregisterInputDeviceListener(deviceListener)
        watchingControllers = false
    }

    @Suppress("DEPRECATION")
    private fun applyImmersive() {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    private companion object {
        const val STATE_KEY = "olho_state"
    }
}
