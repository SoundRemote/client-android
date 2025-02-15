package io.github.soundremote

import io.github.soundremote.data.Hotkey
import io.github.soundremote.util.KeyCode
import io.github.soundremote.util.Mods

/**
 * Creates a [Hotkey] with the specified parameters. Other parameters will have arbitrary default
 * values.
 */
fun getHotkey(
    id: Int = 0,
    keyCode: KeyCode = KeyCode(0x42),
    mods: Mods = Mods(),
    name: String = "Hotkey name",
    favoured: Boolean = false,
    order: Int = 0,
): Hotkey {
    return Hotkey(id, keyCode, mods, name, favoured, order)
}
