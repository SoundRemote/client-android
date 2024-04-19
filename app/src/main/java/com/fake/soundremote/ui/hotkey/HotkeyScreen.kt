package com.fake.soundremote.ui.hotkey

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Right
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fake.soundremote.R
import com.fake.soundremote.ui.components.NavigateUpButton
import com.fake.soundremote.util.Key
import com.fake.soundremote.util.KeyCode
import com.fake.soundremote.util.KeyGroup
import com.fake.soundremote.util.KeyLabel
import com.fake.soundremote.util.toKeyCode

private val sharedMod = Modifier
    .fillMaxWidth()
    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HotkeyScreen(
    state: HotkeyScreenUIState,
    onKeyCodeChange: (KeyCode?) -> Unit,
    onWinChange: (Boolean) -> Unit,
    onCtrlChange: (Boolean) -> Unit,
    onShiftChange: (Boolean) -> Unit,
    onAltChange: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    checkCanSave: () -> Boolean,
    onSave: (keyLabel: String) -> Unit,
    onClose: () -> Unit,
    showSnackbar: (String, SnackbarDuration) -> Unit,
    compactHeight: Boolean,
    modifier: Modifier = Modifier,
) {
    val invalidKeyError = stringResource(R.string.error_invalid_key)
    val context = LocalContext.current

    fun getKeyLabel(keyCode: KeyCode): String {
        keyCode.toLetterOrDigitString()?.let { return it }
        return context.getString(keyCode.keyLabelId()!!)
    }

    Column {
        TopAppBar(
            title = {
                val title = when (state.mode) {
                    HotkeyScreenMode.CREATE -> stringResource(R.string.hotkey_create_title)
                    HotkeyScreenMode.EDIT -> stringResource(R.string.hotkey_edit_title)
                }
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = { NavigateUpButton(onClose) },
            actions = {
                IconButton(
                    onClick = {
                        if (checkCanSave()) {
                            val keyLabel = getKeyLabel(state.keyCode!!)
                            onSave(keyLabel)
                            onClose()
                        } else {
                            showSnackbar(invalidKeyError, SnackbarDuration.Short)
                        }
                    }
                ) {
                    Icon(Icons.Default.Done, stringResource(R.string.save))
                }
            }
        )
        Column(modifier = modifier.verticalScroll(rememberScrollState())) {
            KeySelect(
                keyCode = state.keyCode,
                keyGroupIndex = state.keyGroupIndex,
                onKeyCodeChange = { onKeyCodeChange(it) }
            )
            if (compactHeight) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    ModSelectItem(
                        text = stringResource(R.string.win_checkbox_label),
                        checkedProvider = { state.win },
                        onCheckedChange = { onWinChange(it) },
                    )
                    ModSelectItem(
                        text = stringResource(R.string.ctrl_checkbox_label),
                        checkedProvider = { state.ctrl },
                        onCheckedChange = { onCtrlChange(it) },
                    )
                    ModSelectItem(
                        text = stringResource(R.string.shift_checkbox_label),
                        checkedProvider = { state.shift },
                        onCheckedChange = { onShiftChange(it) },
                    )
                    ModSelectItem(
                        text = stringResource(R.string.alt_checkbox_label),
                        checkedProvider = { state.alt },
                        onCheckedChange = { onAltChange(it) },
                    )
                }
            } else {
                ModSelectItem(
                    text = stringResource(R.string.win_checkbox_label),
                    checkedProvider = { state.win },
                    onCheckedChange = { onWinChange(it) },
                )
                ModSelectItem(
                    text = stringResource(R.string.ctrl_checkbox_label),
                    checkedProvider = { state.ctrl },
                    onCheckedChange = { onCtrlChange(it) },
                )
                ModSelectItem(
                    text = stringResource(R.string.shift_checkbox_label),
                    checkedProvider = { state.shift },
                    onCheckedChange = { onShiftChange(it) },
                )
                ModSelectItem(
                    text = stringResource(R.string.alt_checkbox_label),
                    checkedProvider = { state.alt },
                    onCheckedChange = { onAltChange(it) },
                )
            }
            NameEdit(
                value = state.name,
                onChange = { onNameChange(it) },
            )
        }
    }
}

/**
 * Returns a map of KeyGroup.index to all the Key entities that belong to that KeyGroup
 */
private fun keyOptions(): Map<Int, List<Key>> {
    val result = mutableMapOf<Int, MutableList<Key>>()
    for (keyGroup in KeyGroup.entries) {
        result[keyGroup.index] = mutableListOf()
    }
    for (key in Key.entries) {
        result[key.group.index]?.add(key)
    }
    return result
}

private fun keyGroupToTabIndex(keyGroupIndex: Int): Int {
    return keyGroupIndex
}

@Composable
private fun KeySelect(
    keyCode: KeyCode?,
    keyGroupIndex: Int,
    onKeyCodeChange: (KeyCode?) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyOptions = remember { keyOptions() }
    // Remember entered Char so it could be restored when letter/digit tab selected again
    var selectedChar: Char? by rememberSaveable { mutableStateOf(null) }
    // One time init needed when edit a letter/digit hotkey
    if (selectedChar == null) {
        keyCode?.toLetterOrDigitChar()?.let { selectedChar = it }
    }
    val tabIndex = remember(keyGroupIndex) { keyGroupToTabIndex(keyGroupIndex) }
    val currentKeys = remember(keyGroupIndex) {
        keyOptions.getOrElse(keyGroupIndex) { emptyList() }
    }

    Column(
        modifier = modifier,
    ) {
        ScrollableTabRow(
            selectedTabIndex = tabIndex,
        ) {
            // adjust icon size according to device's font size
            val tabIconSize = with(LocalDensity.current) { 24.sp.toDp() }
            for (keyGroup in KeyGroup.entries) {
                val onTabClick = if (keyGroup.index == KeyGroup.LETTER_DIGIT.index) {
                    { onKeyCodeChange(selectedChar?.toKeyCode()) }
                } else {
                    { onKeyCodeChange(keyOptions[keyGroup.index]!![0].keyCode) }
                }
                Tab(
                    text = { Text(text = stringResource(keyGroup.nameStringId)) },
                    icon = {
                        when (keyGroup.label) {
                            is KeyLabel.Icon -> {
                                Icon(
                                    painter = painterResource(keyGroup.label.iconId),
                                    contentDescription = null,
                                    modifier = Modifier.size(tabIconSize),
                                )
                            }

                            is KeyLabel.String -> {
                                Text(text = stringResource(keyGroup.label.stringId))
                            }
                        }
                    },
                    selected = tabIndex == keyGroupToTabIndex(keyGroup.index),
                    onClick = onTabClick,
                )
            }
        }
        AnimatedContent(
            targetState = tabIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideIntoContainer(Left) + fadeIn())
                        .togetherWith(slideOutOfContainer(Right) + fadeOut())
                } else {
                    (slideIntoContainer(Right) + fadeIn())
                        .togetherWith(slideOutOfContainer(Left) + fadeOut())
                }
            },
            label = "Key group select",
        ) { newTabIndex ->
            if (newTabIndex == keyGroupToTabIndex(KeyGroup.LETTER_DIGIT.index)) {
                val keyEditText: String = keyCode?.toLetterOrDigitChar()?.toString() ?: ""
                val onKeyEditChange: (String) -> Unit = { newText ->
                    if (newText.isBlank()) {
                        selectedChar = null
                        onKeyCodeChange(null)
                    } else {
                        val currentChar = newText.last()
                        currentChar.toKeyCode()?.let {
                            selectedChar = currentChar
                            onKeyCodeChange(it)
                        }
                    }
                }
                OutlinedTextField(
                    value = keyEditText,
                    onValueChange = onKeyEditChange,
                    modifier = sharedMod,
                    label = { Text(stringResource(R.string.hotkey_key_edit_label)) },
                    supportingText = { Text(stringResource(R.string.hotkey_key_edit_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    singleLine = true,
                )
            } else {
                KeySelectCombobox(
                    keys = currentKeys,
                    selectedKeyCode = keyCode,
                    onSelectKey = onKeyCodeChange,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeySelectCombobox(
    keys: List<Key>,
    selectedKeyCode: KeyCode?,
    onSelectKey: (KeyCode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val keyCaption: String = when {
        keys.isEmpty() -> ""
        selectedKeyCode == null -> stringResource(keys[0].labelId)
        else -> keys.find { it.keyCode == selectedKeyCode }
            ?.let { stringResource(it.labelId) }
            ?: ""
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.then(sharedMod)
    ) {
        OutlinedTextField(
            value = keyCaption,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            label = { Text(stringResource(R.string.hotkey_key_edit_label)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            keys.forEach { key ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(key.labelId))
                            if (key.descriptionStringId != null) {
                                Text(text = stringResource(key.descriptionStringId))
                            }
                        }
                    },
                    onClick = {
                        onSelectKey(key.keyCode)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun ModSelectItem(
    text: String,
    checkedProvider: () -> Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .toggleable(
                value = checkedProvider(),
                onValueChange = onCheckedChange,
                role = Role.Checkbox,
            )
            .height(56.dp)
            .then(sharedMod),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checkedProvider(),
            onCheckedChange = null,
        )
        Spacer(Modifier.size(16.dp))
        Text(text = text)
    }
}

@Composable
private fun NameEdit(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.then(sharedMod),
        label = { Text(stringResource(R.string.hotkey_name_edit_label)) },
        placeholder = { Text(stringResource(R.string.hotkey_name_edit_placeholder)) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear)
                    )
                }
            }
        },
        singleLine = true,
    )
}

@Preview(showBackground = true)
@Composable
private fun Portrait() {
    ScreenPreview(false)
}

@Preview(showBackground = true, device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun Landscape() {
    ScreenPreview(true)
}

@Composable
private fun ScreenPreview(compactHeight: Boolean) {
    var win by remember { mutableStateOf(true) }
    var ctrl by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf(true) }
    var alt by remember { mutableStateOf(false) }
    HotkeyScreen(
        state = HotkeyScreenUIState(
            mode = HotkeyScreenMode.EDIT,
            name = "Test name",
            win = win,
            ctrl = ctrl,
            shift = shift,
            alt = alt,
            keyCode = Key.MEDIA_NEXT.keyCode,
            keyGroupIndex = Key.MEDIA_NEXT.group.index
        ),
        onKeyCodeChange = {},
        onWinChange = { win = it },
        onCtrlChange = { ctrl = it },
        onAltChange = { alt = it },
        onShiftChange = { shift = it },
        onClose = {},
        checkCanSave = { false },
        onNameChange = {},
        onSave = {},
        showSnackbar = { _, _ -> },
        compactHeight = compactHeight,
    )
}
