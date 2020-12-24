package org.openrs2.cache

import io.netty.buffer.ByteBuf
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use

public inline class Js5MasterIndex(public val entries: MutableList<Entry> = mutableListOf()) {
    public data class Entry(public var version: Int, public var checksum: Int)

    public fun write(buf: ByteBuf) {
        for (entry in entries) {
            buf.writeInt(entry.checksum)
            buf.writeInt(entry.version)
        }
    }

    public companion object {
        public fun create(store: Store): Js5MasterIndex {
            val index = Js5MasterIndex()

            var nextArchive = 0
            for (archive in store.list(Js5Archive.ARCHIVESET)) {
                /*
                 * Fill in gaps with zeroes. I think this is consistent with
                 * the official implementation: the TFU client warns that
                 * entries with a zero CRC are probably invalid.
                 */
                for (i in nextArchive until archive) {
                    index.entries += Entry(0, 0)
                }

                val entry = store.read(Js5Archive.ARCHIVESET, archive).use { buf ->
                    val checksum = buf.crc32()
                    val version = Js5Index.read(buf).version
                    // TODO(gpe): should we throw an exception if there are trailing bytes here?
                    Entry(version, checksum)
                }

                index.entries += entry
                nextArchive = archive + 1
            }

            return index
        }

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
