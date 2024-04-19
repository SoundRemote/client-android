package com.fake.soundremote.ui.hotkey

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fake.soundremote.data.Hotkey
import com.fake.soundremote.data.HotkeyRepository
import com.fake.soundremote.util.Key
import com.fake.soundremote.util.KeyCode
import com.fake.soundremote.util.KeyGroup
import com.fake.soundremote.util.ModKey
import com.fake.soundremote.util.Mods
import com.fake.soundremote.util.generateDescription
import com.fake.soundremote.util.isModActive
import dagger.hilt.android.lifecycle.HiltViewModel
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
    savedStateHandle: SavedStateHandle,
    private val hotkeyRepository: HotkeyRepository
) : ViewModel() {
    private var initialHotkey: Hotkey? = null

    private val _hotkeyScreenState: MutableStateFlow<HotkeyScreenUIState> =
        MutableStateFlow(HotkeyScreenUIState())
    val hotkeyScreenState: StateFlow<HotkeyScreenUIState>
        get() = _hotkeyScreenState

    init {
        HotkeyEditArgs(savedStateHandle).hotkeyId?.let { id ->
            viewModelScope.launch {
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
        }
    }

    private fun getKeyGroupIndex(keyCode: KeyCode?): Int {
        if (keyCode == null) return KeyGroup.LETTER_DIGIT.index
        val key = Key.entries.find { it.keyCode == keyCode }
        return key?.group?.index ?: KeyGroup.LETTER_DIGIT.index
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

    fun canSave(): Boolean {
        return hotkeyScreenState.value.keyCode != null
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
                val id = hotkeyToUpdate.id
                val favoured = hotkeyToUpdate.isFavoured
                val order = hotkeyToUpdate.order
                val hotkey = Hotkey(id, currentKeyCode, mods, name, favoured, order)
                viewModelScope.launch {
                    hotkeyRepository.update(hotkey)
                }
            }
        }
    }
}
