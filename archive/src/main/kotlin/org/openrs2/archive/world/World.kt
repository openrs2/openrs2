package org.openrs2.archive.world

import io.netty.buffer.ByteBuf
import org.openrs2.buffer.readString

public data class World(
    public val id: Int,
    public val flags: Int,
    public val hostname: String,
    public val activity: String,
    public val country: Int,
    public val players: Int
) {
    public val isBeta: Boolean
        get() = (flags and FLAG_BETA) != 0

    public companion object {
        private const val FLAG_BETA = 0x10000

        public fun read(buf: ByteBuf): World {
            val id = buf.readUnsignedShort()
            val flags = buf.readInt()
            val hostname = buf.readString()
            val activity = buf.readString()
            val country = buf.readUnsignedByte().toInt()
            val players = buf.readShort().toInt()

            return World(id, flags, hostname, activity, country, players)
        }
    }
}
