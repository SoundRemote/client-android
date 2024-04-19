package com.fake.soundremote.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
class KeysTest {
    class HotkeyDescription {
        private val context = ApplicationProvider.getApplicationContext<Context>()
        private val key = Key.BACK
        private val desc = generateDescription(
            key.keyCode,
            Mods(win = true, ctrl = false, shift = false, alt = true),
        )

        @Test
        fun asString_correctKeyLabel() {
            val expected = context.getString(key.labelId)

            val actual = desc.asString(context)

            assertThat(actual, containsString(expected))
        }

        @Test
        fun asString_correctMods() {
            val actual = desc.asString(context)

            assertThat(actual, containsString(ModKey.WIN.label))
            assertThat(actual, containsString(ModKey.ALT.label))
            assertThat(actual, not(containsString(ModKey.CTRL.label)))
            assertThat(actual, not(containsString(ModKey.SHIFT.label)))
        }
    }
}
