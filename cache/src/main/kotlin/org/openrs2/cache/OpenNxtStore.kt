package org.openrs2.cache

import io.netty.buffer.Unpooled
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use
import org.sqlite.SQLiteDataSource
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection

public object OpenNxtStore {
    public fun unpack(input: Path, output: Store) {
        output.create(Store.ARCHIVESET)

        for (archive in 0..Store.MAX_ARCHIVE) {
            val path = input.resolve("js5-$archive.jcache")
            if (!Files.exists(path)) {
                continue
            }

            val dataSource = SQLiteDataSource()
            dataSource.url = "jdbc:sqlite:$path"

            dataSource.connection.use { connection ->
                unpackArchive(connection, archive, output)
            }
        }
    }

    private fun unpackArchive(connection: Connection, archive: Int, output: Store) {
        connection.prepareStatement("""
            SELECT data, crc
            FROM cache_index
            WHERE key = 1
        """.trimIndent()).use { stmt ->
            stmt.executeQuery().use { rows ->
                if (rows.next()) {
                    val checksum = rows.getInt(2)

                    Unpooled.wrappedBuffer(rows.getBytes(1)).use { buf ->
                        val actualChecksum = buf.crc32()
                        if (actualChecksum != checksum) {
                            throw StoreCorruptException(
                                "Js5Index corrupt (expected checksum $checksum, actual checksum $actualChecksum)"
                            )
                        }

                        output.write(Store.ARCHIVESET, archive, buf)
                    }
                }
            }
        }

        connection.prepareStatement("""
            SELECT key, data, crc, version
            FROM cache
        """.trimIndent()).use { stmt ->
            stmt.executeQuery().use { rows ->
                while (rows.next()) {
                    val group = rows.getInt(1)
                    val checksum = rows.getInt(3)
                    val version = rows.getInt(4) and 0xFFFF

                    Unpooled.wrappedBuffer(rows.getBytes(2)).use { buf ->
                        val actualVersion = VersionTrailer.peek(buf)
                        if (actualVersion != version) {
                            throw StoreCorruptException(
                                "Group corrupt (expected version $version, actual version $actualVersion)"
                            )
                        }

                        val actualChecksum = buf.slice(buf.readerIndex(), buf.writerIndex() - 2).crc32()
                        if (actualChecksum != checksum) {
                            throw StoreCorruptException(
                                "Group corrupt (expected checksum $checksum, actual checksum $actualChecksum)"
                            )
                        }

                        output.write(archive, group, buf)
                    }
                }
            }
        }
    }
}
