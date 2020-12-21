package org.openrs2.cache

import io.netty.buffer.ByteBuf

public inline class Js5MasterIndex(public val entries: MutableList<Entry> = mutableListOf()) {
    public data class Entry(public var version: Int, public var checksum: Int)

    public fun write(buf: ByteBuf) {
        for (entry in entries) {
            buf.writeInt(entry.checksum)
            buf.writeInt(entry.version)
        }
    }

    public companion object {
        public fun read(buf: ByteBuf): Js5MasterIndex {
            require(buf.readableBytes() % 8 == 0)

            val index = Js5MasterIndex()
            while (buf.isReadable) {
                val checksum = buf.readInt()
                val version = buf.readInt()
                index.entries += Entry(version, checksum)
            }
            return index
        }
    }
}
