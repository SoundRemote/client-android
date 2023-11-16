package com.fake.soundremote.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fake.soundremote.ui.util.ListItemHeadline
import com.fake.soundremote.ui.util.ListItemSupport

@Composable
internal fun PreferenceItem(
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 24.dp)
    ) {
        ListItemHeadline(title)
        ListItemSupport(summary)
    }
}
