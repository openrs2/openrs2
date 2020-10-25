package org.openrs2.cache

import io.netty.buffer.ByteBuf

public inline class Js5MasterIndex(public val entries: MutableList<Entry> = mutableListOf()) {
    public class Entry(public var version: Int, public var checksum: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Entry

            if (version != other.version) return false
            if (checksum != other.checksum) return false

            return true
        }

        override fun hashCode(): Int {
            var result = version
            result = 31 * result + checksum
            return result
        }

        override fun toString(): String {
            return "Entry{version=$version, checksum=$checksum}"
        }
    }

    public fun write(buf: ByteBuf) {
        for (entry in entries) {
            buf.writeInt(entry.checksum)
            buf.writeInt(entry.version)
        }
    }

    public companion object {
        public fun read(buf: ByteBuf): Js5MasterIndex {
            check(buf.readableBytes() % 8 == 0)

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
