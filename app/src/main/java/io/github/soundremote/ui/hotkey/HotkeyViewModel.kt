package io.github.soundremote.ui.hotkey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.soundremote.data.Hotkey
import io.github.soundremote.data.HotkeyRepository
import io.github.soundremote.util.Key
import io.github.soundremote.util.KeyCode
import io.github.soundremote.util.KeyGroup
import io.github.soundremote.util.ModKey
import io.github.soundremote.util.Mods
import io.github.soundremote.util.generateDescription
import io.github.soundremote.util.isModActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HotkeyScreenMode { CREATE, EDIT }

data class HotkeyScreenUIState(
    val mode: HotkeyScreenMode = HotkeyScreenMode.CREATE,
    val name: String = "",
    val win: Boolean = false,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
    val keyCode: KeyCode? = null,
    val keyGroupIndex: Int = KeyGroup.LETTER_DIGIT.index,
)

@HiltViewModel
internal class HotkeyViewModel @Inject constructor(
    private val hotkeyRepository: HotkeyRepository
) : ViewModel() {
    private var initialHotkey: Hotkey? = null

    private val _hotkeyScreenState: MutableStateFlow<HotkeyScreenUIState> =
        MutableStateFlow(HotkeyScreenUIState())
    val hotkeyScreenState: StateFlow<HotkeyScreenUIState>
        get() = _hotkeyScreenState

    fun loadHotkey(id: Int) = viewModelScope.launch {
        hotkeyRepository.getById(id)?.let { hotkey ->
            initialHotkey = hotkey
            _hotkeyScreenState.value = HotkeyScreenUIState(
                mode = HotkeyScreenMode.EDIT,
                name = hotkey.name,
                win = hotkey.isModActive(ModKey.WIN),
                ctrl = hotkey.isModActive(ModKey.CTRL),
                shift = hotkey.isModActive(ModKey.SHIFT),
                alt = hotkey.isModActive(ModKey.ALT),
                keyCode = hotkey.keyCode,
                keyGroupIndex = getKeyGroupIndex(hotkey.keyCode),
            )
        }
    }

    private fun getKeyGroupIndex(keyCode: KeyCode?): Int {
        return keyCode?.let { Key.byKeyCode[keyCode]?.group?.index }
            ?: KeyGroup.LETTER_DIGIT.index
    }

    fun updateKeyCode(keyCode: KeyCode?) {
        _hotkeyScreenState.value = _hotkeyScreenState.value
            .copy(keyCode = keyCode, keyGroupIndex = getKeyGroupIndex(keyCode))
    }

    fun updateName(name: String) {
        _hotkeyScreenState.value = _hotkeyScreenState.value.copy(name = name)
    }

    fun updateMod(mod: ModKey, value: Boolean) {
        _hotkeyScreenState.value = when (mod) {
            ModKey.WIN -> _hotkeyScreenState.value.copy(win = value)
            ModKey.CTRL -> _hotkeyScreenState.value.copy(ctrl = value)
            ModKey.SHIFT -> _hotkeyScreenState.value.copy(shift = value)
            ModKey.ALT -> _hotkeyScreenState.value.copy(alt = value)
        }
    }

    fun saveHotkey(keyLabel: String) {
        hotkeyScreenState.value.let { currentState ->
            val currentKeyCode = currentState.keyCode ?: return@let
            val mods = Mods(
                win = currentState.win,
                ctrl = currentState.ctrl,
                shift = currentState.shift,
                alt = currentState.alt,
            )
            val name: String = currentState.name.ifBlank {
                generateDescription(keyLabel, mods)
            }

            val hotkeyToUpdate = initialHotkey
            if (hotkeyToUpdate == null) {
                val hotkey = Hotkey(currentKeyCode, name, mods)
                viewModelScope.launch {
                    hotkeyRepository.insert(hotkey)
                }
            } else {
                val hotkey = hotkeyToUpdate.copy(
                    keyCode = currentKeyCode,
                    mods = mods,
                    name = name,
                )
                viewModelScope.launch {
                    hotkeyRepository.update(hotkey)
                }
            }
        }
    }
}
