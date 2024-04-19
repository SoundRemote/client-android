@file:JvmName("Keys")

package com.fake.soundremote.util

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.fake.soundremote.R
import com.fake.soundremote.data.Hotkey
import com.fake.soundremote.data.HotkeyInfo

/**
 * If this Char is a digit or an english alphabet letter, returns the corresponding virtual-key code.
 * Otherwise, returns null.
 */
fun Char.toKeyCode(): KeyCode? {
    if (this in '0'..'9') {
        return KeyCode(0x30 + this.code - '0'.code)
    }
    val lowercase = this.lowercaseChar()
    return if (lowercase in 'a'..'z') {
        KeyCode(0x41 + lowercase.code - 'a'.code)
    } else null
}

sealed interface HotkeyDescription {
    data class WithString(val text: String) : HotkeyDescription
    data class WithLabelId(val mods: String, @StringRes val labelId: Int) : HotkeyDescription

    @Composable
    fun asString(): String =
        asString(LocalContext.current)

    fun asString(context: Context): String = when (this) {
        is WithString -> text
        is WithLabelId -> mods + context.getString(labelId)
    }
}

/**
 * Generates a [HotkeyDescription] for the [Hotkey]
 *
 * @param hotkey the source [Hotkey]
 * @return description
 */
fun generateDescription(hotkey: Hotkey): HotkeyDescription =
    generateDescription(hotkey.keyCode, hotkey.mods)

fun generateDescription(hotkey: HotkeyInfo): HotkeyDescription =
    generateDescription(hotkey.keyCode, hotkey.mods)

/**
 * Generates a [HotkeyDescription]
 *
 * @param keyCode main key virtual-key code
 * @param mods mods
 * @return description
 */
fun generateDescription(keyCode: KeyCode, mods: Mods): HotkeyDescription {
    keyCode.toLetterOrDigitString()?.let {
        val desc = generateDescription(it, mods)
        return HotkeyDescription.WithString(desc)
    }
    val modsPrefix = mods.toPrefixString()
    return HotkeyDescription.WithLabelId(modsPrefix, keyCode.keyLabelId()!!)
}

fun generateDescription(keyLabel: String, mods: Mods): String =
    mods.toPrefixString() + keyLabel

sealed interface KeyLabel {
    data class String(@StringRes val stringId: Int) : KeyLabel
    data class Icon(@DrawableRes val iconId: Int) : KeyLabel
}

// https://support.microsoft.com/en-us/windows/using-your-keyboard-18b2efc1-9e32-ba5a-0896-676f9f3b994f
// https://learn.microsoft.com/en-us/windows/win32/inputdev/virtual-key-codes
enum class KeyGroup(
    @StringRes val nameStringId: Int,
    val label: KeyLabel,
) {
    LETTER_DIGIT(
        R.string.key_group_letter_digit,
        KeyLabel.String(R.string.key_group_label_letter_digit),
    ),
    MEDIA(R.string.key_group_media, KeyLabel.Icon(R.drawable.ic_play_pause)),
    TYPING(R.string.key_group_typing, KeyLabel.String(R.string.key_group_label_typing)),
    CONTROL(R.string.key_group_control, KeyLabel.String(R.string.key_group_label_control)),
    NAVIGATION(R.string.key_group_navigation, KeyLabel.String(R.string.key_group_label_navigation)),
    NUM_PAD(R.string.key_group_numpad, KeyLabel.String(R.string.key_group_label_numpad)),
    FUNCTION(R.string.key_group_function, KeyLabel.String(R.string.key_group_label_function));

    val index: Int
        get() = ordinal
}

/**
 * A keyboard key.
 *
 * @param keyCode Windows Virtual-Key code
 * @param labelId Key label resource string id
 * @param group [KeyGroup] this Key belongs to
 * @param descriptionStringId optional description resource string id
 */
enum class Key(
    val keyCode: KeyCode,
    @StringRes
    val labelId: Int,
    val group: KeyGroup,
    @StringRes
    val descriptionStringId: Int? = null
) {
    // Media
    MEDIA_PLAY_PAUSE(KeyCode(0xB3), R.string.key_media_play_pause, KeyGroup.MEDIA),
    MEDIA_NEXT(KeyCode(0xB0), R.string.key_media_next, KeyGroup.MEDIA),
    MEDIA_PREV(KeyCode(0xB1), R.string.key_media_prev, KeyGroup.MEDIA),
    MEDIA_STOP(KeyCode(0xB2), R.string.key_media_stop, KeyGroup.MEDIA),
    MEDIA_VOLUME_MUTE(KeyCode(0xAD), R.string.key_media_volume_mute, KeyGroup.MEDIA),
    MEDIA_VOLUME_DOWN(KeyCode(0xAE), R.string.key_media_volume_down, KeyGroup.MEDIA),
    MEDIA_VOLUME_UP(KeyCode(0xAF), R.string.key_media_volume_up, KeyGroup.MEDIA),

    // Typing
    TILDE(KeyCode(0xC0), R.string.key_tilde, KeyGroup.TYPING, R.string.key_desc_tilde),
    MINUS(KeyCode(0xBD), R.string.key_minus, KeyGroup.TYPING, R.string.key_desc_minus),
    PLUS(KeyCode(0xBB), R.string.key_plus, KeyGroup.TYPING, R.string.key_desc_plus),
    SQUARE_BRACKET_LEFT(
        KeyCode(0xDB),
        R.string.key_square_bracket_left,
        KeyGroup.TYPING,
        R.string.key_desc_bracket_left
    ),
    SQUARE_BRACKET_RIGHT(
        KeyCode(0xDD),
        R.string.key_square_bracket_right,
        KeyGroup.TYPING,
        R.string.key_desc_bracket_right
    ),
    SEMICOLON(KeyCode(0xBA), R.string.key_semicolon, KeyGroup.TYPING, R.string.key_desc_semicolon),
    QUOTE(KeyCode(0xDE), R.string.key_quote, KeyGroup.TYPING, R.string.key_desc_quote),
    BACKSLASH(KeyCode(0xDC), R.string.key_backslash, KeyGroup.TYPING, R.string.key_desc_backslash),
    COMMA(KeyCode(0xBC), R.string.key_comma, KeyGroup.TYPING, R.string.key_desc_comma),
    PERIOD(KeyCode(0xBE), R.string.key_period, KeyGroup.TYPING, R.string.key_desc_period),
    SLASH(KeyCode(0xBF), R.string.key_slash, KeyGroup.TYPING, R.string.key_desc_slash),

    // Control
    TAB(KeyCode(0x09), R.string.key_tab, KeyGroup.CONTROL),
    SPACE(KeyCode(0x20), R.string.key_space, KeyGroup.CONTROL),
    BACK(KeyCode(0x08), R.string.key_back, KeyGroup.CONTROL),
    ENTER(KeyCode(0x0D), R.string.key_enter, KeyGroup.CONTROL),
    CAPITAL(KeyCode(0x14), R.string.key_capital, KeyGroup.CONTROL),
    ESC(KeyCode(0x1B), R.string.key_esc, KeyGroup.CONTROL),
    PRINT_SCREEN(KeyCode(0x2C), R.string.key_print_screen, KeyGroup.CONTROL),
    SCROLL_LOCK(KeyCode(0x91), R.string.key_scroll_lock, KeyGroup.CONTROL),
    PAUSE(KeyCode(0x13), R.string.key_pause, KeyGroup.CONTROL),

    // Navigation
    INSERT(KeyCode(0x2D), R.string.key_insert, KeyGroup.NAVIGATION),
    DELETE(KeyCode(0x2E), R.string.key_delete, KeyGroup.NAVIGATION),
    HOME(KeyCode(0x24), R.string.key_home, KeyGroup.NAVIGATION),
    END(KeyCode(0x23), R.string.key_end, KeyGroup.NAVIGATION),
    PAGE_UP(KeyCode(0x21), R.string.key_page_up, KeyGroup.NAVIGATION),
    PAGE_DOWN(KeyCode(0x22), R.string.key_page_down, KeyGroup.NAVIGATION),
    UP(KeyCode(0x26), R.string.key_up, KeyGroup.NAVIGATION, R.string.key_desc_up),
    LEFT(KeyCode(0x25), R.string.key_left, KeyGroup.NAVIGATION, R.string.key_desc_left),
    RIGHT(KeyCode(0x27), R.string.key_right, KeyGroup.NAVIGATION, R.string.key_desc_right),
    DOWN(KeyCode(0x28), R.string.key_down, KeyGroup.NAVIGATION, R.string.key_desc_down),

    // Numpad
    NUM_0(KeyCode(0x60), R.string.key_num_0, KeyGroup.NUM_PAD),
    NUM_1(KeyCode(0x61), R.string.key_num_1, KeyGroup.NUM_PAD),
    NUM_2(KeyCode(0x62), R.string.key_num_2, KeyGroup.NUM_PAD),
    NUM_3(KeyCode(0x63), R.string.key_num_3, KeyGroup.NUM_PAD),
    NUM_4(KeyCode(0x64), R.string.key_num_4, KeyGroup.NUM_PAD),
    NUM_5(KeyCode(0x65), R.string.key_num_5, KeyGroup.NUM_PAD),
    NUM_6(KeyCode(0x66), R.string.key_num_6, KeyGroup.NUM_PAD),
    NUM_7(KeyCode(0x67), R.string.key_num_7, KeyGroup.NUM_PAD),
    NUM_8(KeyCode(0x67), R.string.key_num_8, KeyGroup.NUM_PAD),
    NUM_9(KeyCode(0x69), R.string.key_num_9, KeyGroup.NUM_PAD),
    NUM_MULTIPLY(KeyCode(0x6A), R.string.key_num_multiply, KeyGroup.NUM_PAD),
    NUM_ADD(KeyCode(0x6B), R.string.key_num_add, KeyGroup.NUM_PAD),
    NUM_SUBTRACT(KeyCode(0x6D), R.string.key_num_subtract, KeyGroup.NUM_PAD),
    NUM_DECIMAL(KeyCode(0x6E), R.string.key_num_decimal, KeyGroup.NUM_PAD),
    NUM_DIVIDE(KeyCode(0x6F), R.string.key_num_divide, KeyGroup.NUM_PAD),
    NUM_LOCK(KeyCode(0x90), R.string.key_num_lock, KeyGroup.NUM_PAD),

    // Function
    F1(KeyCode(0x70), R.string.key_f1, KeyGroup.FUNCTION),
    F2(KeyCode(0x71), R.string.key_f2, KeyGroup.FUNCTION),
    F3(KeyCode(0x72), R.string.key_f3, KeyGroup.FUNCTION),
    F4(KeyCode(0x73), R.string.key_f4, KeyGroup.FUNCTION),
    F5(KeyCode(0x74), R.string.key_f5, KeyGroup.FUNCTION),
    F6(KeyCode(0x75), R.string.key_f6, KeyGroup.FUNCTION),
    F7(KeyCode(0x76), R.string.key_f7, KeyGroup.FUNCTION),
    F8(KeyCode(0x77), R.string.key_f8, KeyGroup.FUNCTION),
    F9(KeyCode(0x78), R.string.key_f9, KeyGroup.FUNCTION),
    F10(KeyCode(0x79), R.string.key_f10, KeyGroup.FUNCTION),
    F11(KeyCode(0x7A), R.string.key_f11, KeyGroup.FUNCTION),
    F12(KeyCode(0x7B), R.string.key_f12, KeyGroup.FUNCTION);

    override fun toString(): String {
        return name
    }
}

enum class ModKey(val bitField: Int, val label: String) {
    WIN(1, "Win"),
    CTRL(1 shl 1, "Ctrl"),
    SHIFT(1 shl 2, "Shift"),
    ALT(1 shl 3, "Alt");
}

/**
 * Checks this [Hotkey] for the specific mod key
 *
 * @param mod [ModKey] to check
 * @return true if this [Hotkey] has the specified mod key active
 */
fun Hotkey.isModActive(mod: ModKey): Boolean {
    return mods.isModActive(mod)
}

@JvmInline
value class KeyCode(val value: Int) {

    /**
     * If this [KeyCode] is a virtual-key code for a digit or an english alphabet letter key,
     * returns the `Char` representing the corresponding character. Otherwise, returns null.
     *
     * @return `Char` or null
     */
    fun toLetterOrDigitChar(): Char? {
        if (value in 0x30..0x39) {
            return ('0'.code + value - 0x30).toChar()
        }
        return if (value in 0x41..0x5A) {
            ('a'.code + value - 0x41).toChar()
        } else null
    }

    /**
     * Same as [toLetterOrDigitChar] but converts `Char` into an uppercase `String`.
     *
     * @return `String` or null
     */
    fun toLetterOrDigitString(): String? =
        toLetterOrDigitChar()?.uppercase()

    /**
     * If this [KeyCode] is __not__ a virtual-key code for a digit or an english alphabet letter
     * key, returns resource id of the string containing its label. Returns null if there is no
     * [Key] with such [KeyCode].
     *
     * @return label string resource id or null
     */
    @StringRes
    fun keyLabelId(): Int? =
        Key.entries.find { it.keyCode == this }?.labelId
}

@JvmInline
value class Mods(val value: Int = 0) {
    constructor(win: Boolean, ctrl: Boolean, shift: Boolean, alt: Boolean) : this(
        (if (win) ModKey.WIN.bitField else 0)
                or (if (ctrl) ModKey.CTRL.bitField else 0)
                or (if (shift) ModKey.SHIFT.bitField else 0)
                or (if (alt) ModKey.ALT.bitField else 0)
    )

    fun isModActive(mod: ModKey): Boolean {
        return value and mod.bitField != 0
    }

    /**
     * Creates a string with all the active mods separated by " + " with a trailing " + ".
     * For example: "Ctrl + Shift + ". Returns an empty string if there are no active mods.
     *
     * @return prefix string
     */
    fun toPrefixString(): String = ModKey.entries
        .filter { isModActive(it) }
        .fold("") { result, mod -> result + "${mod.label} + " }
}
