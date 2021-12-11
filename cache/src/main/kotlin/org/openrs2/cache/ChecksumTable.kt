package org.openrs2.cache

import io.netty.buffer.ByteBuf
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use

public class ChecksumTable(
    public val entries: MutableList<Int> = mutableListOf()
) {
    public fun write(buf: ByteBuf) {
        for (entry in entries) {
            buf.writeInt(entry)
        }

        var checksum = 1234
        for (entry in entries) {
            checksum = (checksum shl 1) + entry
        }

        buf.writeInt(checksum)
    }

    public companion object {
        public fun create(store: Store): ChecksumTable {
            val table = ChecksumTable()

            var nextArchive = 0
            for (archive in store.list(0)) {
                val entry = store.read(0, archive).use { buf ->
                    buf.crc32()
                }

                for (i in nextArchive until archive) {
                    table.entries += 0
                }

                table.entries += entry
                nextArchive = archive + 1
            }

            return table
        }

        public fun read(buf: ByteBuf): ChecksumTable {
            val table = ChecksumTable()

            var expectedChecksum = 1234

            while (buf.readableBytes() >= 8) {
                val entry = buf.readInt()
                table.entries += entry

                expectedChecksum = (expectedChecksum shl 1) + entry
            }

            val actualChecksum = buf.readInt()
            require(expectedChecksum == actualChecksum)

            return table
        }
    }
}
