package app.olhonasnuvens.tv.input

import app.olhonasnuvens.tv.game.BackResult
import app.olhonasnuvens.tv.game.GameSession

/**
 * Códigos iguais aos de android.view.KeyEvent. Ficam aqui para a lógica
 * de controle ser testada sem o SDK Android.
 */
object TvKeyCodes {
    const val BACK = 4
    const val DPAD_UP = 19
    const val DPAD_DOWN = 20
    const val DPAD_LEFT = 21
    const val DPAD_RIGHT = 22
    const val DPAD_CENTER = 23
    const val SPACE = 62
    const val ENTER = 66
    const val MEDIA_PLAY_PAUSE = 85
    const val MENU = 82
    const val BUTTON_A = 96
    const val BUTTON_B = 97
    const val BUTTON_START = 108
    const val BUTTON_SELECT = 109
    const val ESCAPE = 111
    const val NUMPAD_ENTER = 160
}

enum class PadAction {
    CONFIRM,
    UP,
    DOWN,
    BACK,
    PAUSE,
}

sealed class InputOutcome {
    data object Unhandled : InputOutcome()
    data object Consumed : InputOutcome()
    data class Action(val action: PadAction) : InputOutcome()
}

fun classifyKey(keyCode: Int): PadAction? = when (keyCode) {
    TvKeyCodes.DPAD_CENTER,
    TvKeyCodes.ENTER,
    TvKeyCodes.NUMPAD_ENTER,
    TvKeyCodes.BUTTON_A,
    TvKeyCodes.BUTTON_SELECT,
    TvKeyCodes.SPACE,
    -> PadAction.CONFIRM
    TvKeyCodes.DPAD_UP -> PadAction.UP
    TvKeyCodes.DPAD_DOWN -> PadAction.DOWN
    TvKeyCodes.BACK,
    TvKeyCodes.BUTTON_B,
    TvKeyCodes.ESCAPE,
    -> PadAction.BACK
    TvKeyCodes.BUTTON_START,
    TvKeyCodes.MEDIA_PLAY_PAUSE,
    TvKeyCodes.MENU,
    -> PadAction.PAUSE
    else -> null
}

fun isHorizontalKey(keyCode: Int): Boolean {
    return keyCode == TvKeyCodes.DPAD_LEFT || keyCode == TvKeyCodes.DPAD_RIGHT
}

class InputDecoder(
    private val flapGate: RepeatGate = RepeatGate(FLAP_INTERVAL_MS),
    private val navGate: RepeatGate = RepeatGate(NAV_INTERVAL_MS),
) {
    fun decode(
        keyCode: Int,
        repeatCount: Int,
        eventTimeMillis: Long,
        playing: Boolean,
    ): InputOutcome {
        if (isHorizontalKey(keyCode)) return InputOutcome.Consumed
        val action = classifyKey(keyCode) ?: return InputOutcome.Unhandled
        val gate = if (action == PadAction.CONFIRM && playing) flapGate else navGate
        if (!gate.accept(eventTimeMillis, repeatCount)) return InputOutcome.Consumed
        return InputOutcome.Action(action)
    }

    companion object {
        const val FLAP_INTERVAL_MS = 110L
        const val NAV_INTERVAL_MS = 180L
    }
}

object SessionInput {
    /** @return true quando a activity deve fechar. */
    fun apply(action: PadAction, session: GameSession): Boolean {
        return when (action) {
            PadAction.CONFIRM -> {
                session.onConfirm()
                false
            }
            PadAction.UP -> {
                session.onUp()
                false
            }
            PadAction.DOWN -> {
                session.onDown()
                false
            }
            PadAction.BACK -> session.onBack() == BackResult.FINISH
            PadAction.PAUSE -> {
                session.togglePause()
                false
            }
        }
    }
}
