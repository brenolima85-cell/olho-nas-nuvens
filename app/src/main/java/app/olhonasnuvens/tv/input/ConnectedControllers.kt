package app.olhonasnuvens.tv.input

/**
 * Máscaras iguais às de android.view.InputDevice. A comparação é
 * `(sources and mask) == mask`, como `InputDevice` / `MotionEvent.isFromSource`.
 * Teclado e mouse sozinhos não são controle de jogo. Um remoto com D-pad, sim.
 */
object PlayControllerSources {
    const val KEYBOARD = 0x00000101
    const val DPAD = 0x00000201
    const val GAMEPAD = 0x00000401
    const val JOYSTICK = 0x01000010
    const val MOUSE = 0x00002002

    fun matches(sources: Int, mask: Int): Boolean = (sources and mask) == mask

    fun isStickSource(sources: Int): Boolean {
        return matches(sources, JOYSTICK) || matches(sources, GAMEPAD)
    }

    fun isPlayController(sources: Int): Boolean {
        return isStickSource(sources) || matches(sources, DPAD)
    }
}

data class ControllerUpdate(
    val lostPlayController: Boolean,
)

/**
 * Guarda os ids enquanto o aparelho ainda existe. Na remoção o sistema
 * já não deixa consultar o dispositivo.
 */
class ConnectedControllers {
    private val playIds = mutableSetOf<Int>()

    fun update(deviceId: Int, sources: Int): ControllerUpdate {
        val play = PlayControllerSources.isPlayController(sources)
        val had = playIds.contains(deviceId)
        if (play) playIds.add(deviceId) else playIds.remove(deviceId)
        return ControllerUpdate(lostPlayController = had && !play)
    }

    fun remove(deviceId: Int): Boolean = playIds.remove(deviceId)

    fun tracks(deviceId: Int): Boolean = playIds.contains(deviceId)
}
