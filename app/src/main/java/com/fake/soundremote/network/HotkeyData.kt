package com.fake.soundremote.network

import com.fake.soundremote.util.Net.putUByte
import com.fake.soundremote.util.PacketKeyType
import com.fake.soundremote.util.PacketModsType
import java.nio.ByteBuffer

data class HotkeyData(val keyCode: PacketKeyType, val mods: PacketModsType) : PacketData {

    override fun write(dest: ByteBuffer) {
        require(dest.remaining() >= SIZE)
        dest.putUByte(keyCode)
        dest.putUByte(mods)
    }

    companion object {
        /**
         * unsigned 8bit    Virtual-key code
         *
         * unsigned 8bit    Bit field of the mod keys
         */
        const val SIZE = 2
    }
}
