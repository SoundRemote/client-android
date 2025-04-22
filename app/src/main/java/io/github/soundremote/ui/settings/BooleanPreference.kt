package io.github.soundremote.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.soundremote.ui.components.ListItemHeadline
import io.github.soundremote.ui.components.ListItemSupport

@Composable
internal fun BooleanPreference(
    title: String,
    summary: String,
    value: Boolean,
    onPreferenceChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = value,
                role = Role.Switch,
                onValueChange = onPreferenceChange,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // 56 + 8 * 2(vertical padding) = 72 (recommended height for a two lines list item container)
            .heightIn(min = 56.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            ListItemHeadline(title)
            ListItemSupport(summary)
        }
        Switch(
            checked = value,
            onCheckedChange = onPreferenceChange,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BooleanPreferencePreview1() {
    var state by remember { mutableStateOf(false) }
    BooleanPreference(
        title = "Pref title",
        summary = "a ".repeat(100),
        value = state,
        onPreferenceChange = { state = it },
    )
}

@Preview(showBackground = true)
@Composable
private fun BooleanPreferencePreview2() {
    var state by remember { mutableStateOf(false) }
    BooleanPreference(
        title = "Pref title",
        summary = "1 line summary",
        value = state,
        onPreferenceChange = { state = it },
    )
}
