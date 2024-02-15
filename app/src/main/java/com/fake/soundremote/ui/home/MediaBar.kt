package com.fake.soundremote.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.fake.soundremote.R
import com.fake.soundremote.util.Key

@Composable
fun MediaBar(
    onKeyPress: (Key) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        MediaButton(R.drawable.ic_play_pause, R.string.media_play_pause) {
            onKeyPress(Key.MEDIA_PLAY_PAUSE)
        }
        MediaButton(R.drawable.ic_stop, R.string.media_stop) {
            onKeyPress(Key.MEDIA_STOP)
        }
        MediaButton(R.drawable.ic_skip_previous, R.string.media_previous) {
            onKeyPress(Key.MEDIA_PREV)
        }
        MediaButton(R.drawable.ic_skip_next, R.string.media_next) {
            onKeyPress(Key.MEDIA_NEXT)
        }
        MediaButton(R.drawable.ic_volume_mute, R.string.media_mute) {
            onKeyPress(Key.MEDIA_VOLUME_MUTE)
        }
        MediaButton(R.drawable.ic_volume_down, R.string.media_volume_down) {
            onKeyPress(Key.MEDIA_VOLUME_DOWN)
        }
        MediaButton(R.drawable.ic_volume_up, R.string.media_volume_up) {
            onKeyPress(Key.MEDIA_VOLUME_UP)
        }
    }
}

@Composable
private fun MediaButton(
    @DrawableRes icon: Int,
    @StringRes description: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
        )
    }
}

@Preview
@Composable
private fun MediaBarPreview() {
    MediaBar {}
}