package org.openrs2.archive.world

import io.netty.buffer.ByteBuf

public data class WorldList(
    public val worlds: List<World>
) {
    public companion object {
        public fun read(buf: ByteBuf): WorldList {
            buf.skipBytes(4)

            val count = buf.readUnsignedShort()
            val worlds = buildList(count) {
                for (i in 0 until count) {
                    add(World.read(buf))
                }
            }

            return WorldList(worlds)
        }
    }
}
