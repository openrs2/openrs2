package org.openrs2.cache

import io.netty.buffer.ByteBuf
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use

public class ChecksumTable(
    public var format: ChecksumTableFormat,
    public val entries: MutableList<Int> = mutableListOf()
) {
    public fun write(buf: ByteBuf) {
        for (entry in entries) {
            buf.writeInt(entry)
        }

        if (format >= ChecksumTableFormat.CHECKSUM) {
            var checksum = 1234
            for (entry in entries) {
                checksum = (checksum shl 1) + entry
            }

            buf.writeInt(checksum)
        }
    }

    public companion object {
        @JvmStatic
        public fun create(store: Store, format: ChecksumTableFormat): ChecksumTable {
            val table = ChecksumTable(format)

            var nextArchive = 0
            for (archive in store.list(0)) {
                val entry = try {
                    store.read(0, archive).use { buf ->
                        buf.crc32()
                    }
                } catch (ex: StoreCorruptException) {
                    // see the equivalent comment in Js5MasterIndex::create
                    continue
                }

                for (i in nextArchive until archive) {
                    table.entries += 0
                }

                table.entries += entry
                nextArchive = archive + 1
            }

            return table
        }

        @JvmStatic
        public fun read(buf: ByteBuf): ChecksumTable {
            require(buf.readableBytes() % 4 == 0)

            val format = if (buf.readableBytes() >= 40) ChecksumTableFormat.CHECKSUM else ChecksumTableFormat.ORIGINAL
            val table = ChecksumTable(format)

            if (format >= ChecksumTableFormat.CHECKSUM) {
                var expectedChecksum = 1234

                while (buf.readableBytes() >= 8) {
                    val entry = buf.readInt()
                    table.entries += entry

                    expectedChecksum = (expectedChecksum shl 1) + entry
                }

                val actualChecksum = buf.readInt()
                require(expectedChecksum == actualChecksum)
            } else {
                while (buf.readableBytes() >= 4) {
                    table.entries += buf.readInt()
                }
            }

            return table
        }
    }
}
