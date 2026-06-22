package io.github.soundremote.service

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
class StreamTimeline(private val mediaItem: MediaItem) : Timeline() {

    override fun getWindowCount() = 1

    override fun getWindow(
        windowIndex: Int,
        window: Window,
        defaultPositionProjectionUs: Long,
    ): Window {
        return window.set(
            Window.SINGLE_WINDOW_UID,
            mediaItem,
            null,
            C.TIME_UNSET,
            C.TIME_UNSET,
            C.TIME_UNSET,
            false,
            false,
            null,
            C.TIME_UNSET,
            0L,
            0,
            0,
            0,
        )
    }

    override fun getPeriodCount() = 1

    override fun getPeriod(periodIndex: Int, period: Period, setIds: Boolean): Period {
        require(periodIndex == 0)
        val uid = if (setIds) periodId else null
        return period.set(null, uid, 0, 0L, 0)
    }

    private val periodId = Any()

    override fun getIndexOfPeriod(uid: Any) =
        if (uid == periodId) {
            0
        } else {
            C.INDEX_UNSET
        }

    override fun getUidOfPeriod(periodIndex: Int): Any {
        require(periodIndex == 0)
        return periodId
    }
}