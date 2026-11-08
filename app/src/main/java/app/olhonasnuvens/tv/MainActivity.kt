package app.olhonasnuvens.tv

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import app.olhonasnuvens.tv.game.GameSession
import app.olhonasnuvens.tv.game.RandomGapSource
import app.olhonasnuvens.tv.game.StateCodec
import app.olhonasnuvens.tv.persist.LocalBestScores
import kotlin.random.Random

class MainActivity : Activity() {
    private lateinit var session: GameSession
    private lateinit var gameView: GameView

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
        gameView.onHostResume()
    }

    override fun onPause() {
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

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::gameView.isInitialized && gameView.handleBackFallback()) return
        super.onBackPressed()
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
