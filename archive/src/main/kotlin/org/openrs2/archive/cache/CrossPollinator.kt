package org.openrs2.archive.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.Unpooled
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5CompressionType
import org.openrs2.db.Database
import java.sql.Connection
import java.util.zip.GZIPInputStream

@Singleton
public class CrossPollinator @Inject constructor(
    private val database: Database,
    private val alloc: ByteBufAllocator,
    private val importer: CacheImporter
) {
    public suspend fun crossPollinate() {
        database.execute { connection ->
            for ((index, archive) in OLD_TO_NEW_ENGINE) {
                crossPollinate(connection, index, archive);
            }
        }
    }

    private fun crossPollinate(connection: Connection, index: Int, archive: Int) {
        val scopeId: Int

        connection.prepareStatement(
            """
            SELECT id
            FROM scopes
            WHERE name = 'runescape'
        """.trimIndent()
        ).use { stmt ->
            stmt.executeQuery().use { rows ->
                check(rows.next())

                scopeId = rows.getInt(1)
            }
        }

        val groups = mutableListOf<CacheImporter.Group>()
        val files = mutableListOf<CacheImporter.File>()

        try {
            connection.prepareStatement(
                """
                SELECT
                    new.group_id AS id,
                    old.version AS old_version,
                    old.crc32 AS old_crc32,
                    b.data AS old_data,
                    new.version AS new_version,
                    new.crc32 AS new_crc32,
                    c.data AS new_data
                FROM (
                    SELECT DISTINCT vf.index_id, vf.file_id, vf.version, vf.crc32
                    FROM version_list_files vf
                    WHERE vf.blob_id IN (
                        SELECT v.blob_id
                        FROM version_lists v
                        JOIN resolved_archives a ON a.blob_id = v.blob_id AND a.archive_id = 5
                    ) AND vf.index_id = ?
                ) old
                JOIN (
                    SELECT DISTINCT ig.group_id, ig.version, ig.crc32
                    FROM index_groups ig
                    WHERE ig.container_id IN (
                        SELECT i.container_id
                        FROM resolved_indexes i
                        WHERE i.scope_id = ? AND i.archive_id = ?
                    )
                ) new ON old.file_id = new.group_id AND old.version = new.version + 1
                LEFT JOIN resolve_file(old.index_id, old.file_id, old.version, old.crc32) b ON TRUE
                LEFT JOIN resolve_group(?, ?::uint1, new.group_id, new.crc32, new.version) c ON TRUE
                WHERE (b.data IS NULL AND c.data IS NOT NULL) OR (b.data IS NOT NULL AND c.data IS NULL)
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, index)
                stmt.setInt(2, scopeId)
                stmt.setInt(3, archive)
                stmt.setInt(4, scopeId)
                stmt.setInt(5, archive)

                stmt.executeQuery().use { rows ->
                    while (rows.next()) {
                        val id = rows.getInt(1)
                        val oldVersion = rows.getInt(2)
                        val oldChecksum = rows.getInt(3)
                        val newVersion = rows.getInt(5)
                        val newChecksum = rows.getInt(6)

                        val oldData = rows.getBytes(4)
                        if (oldData != null) {
                            Unpooled.wrappedBuffer(oldData).use { oldBuf ->
                                fileToGroup(oldBuf, newChecksum).use { newBuf ->
                                    if (newBuf != null) {
                                        val uncompressed = Js5Compression.uncompressUnlessEncrypted(newBuf.slice())
                                        groups += CacheImporter.Group(
                                            archive,
                                            id,
                                            newBuf.retain(),
                                            uncompressed,
                                            newVersion,
                                            false
                                        )
                                    }
                                }
                            }
                        }

                        val newData = rows.getBytes(7)
                        if (newData != null) {
                            Unpooled.wrappedBuffer(newData).use { newBuf ->
                                val oldBuf = groupToFile(newBuf, oldChecksum)
                                if (oldBuf != null) {
                                    files += CacheImporter.File(index, id, oldBuf, oldVersion)
                                }
                            }
                        }
                    }
                }
            }

            if (groups.isEmpty() && files.isEmpty()) {
                return
            }

            importer.prepare(connection)

            val sourceId = importer.addSource(
                connection,
                type = CacheImporter.SourceType.CROSS_POLLINATION,
                cacheId = null,
                gameId = null,
                buildMajor = null,
                buildMinor = null,
                timestamp = null,
                name = null,
                description = null,
                url = null,
            )

            if (groups.isNotEmpty()) {
                importer.addGroups(connection, scopeId, sourceId, groups)
            }

            if (files.isNotEmpty()) {
                importer.addFiles(connection, sourceId, files)
            }
        } finally {
            groups.forEach(CacheImporter.Group::release)
            files.forEach(CacheImporter.File::release)
        }
    }

    private fun getUncompressedLength(buf: ByteBuf): Int {
        GZIPInputStream(ByteBufInputStream(buf)).use { input ->
            var len = 0
            val temp = ByteArray(4096)

            while (true) {
                val n = input.read(temp)
                if (n == -1) {
                    break
                }
                len += n
            }

            return len
        }
    }

    private fun fileToGroup(input: ByteBuf, expectedChecksum: Int): ByteBuf? {
        val len = input.readableBytes()
        val lenWithHeader = len + JS5_COMPRESSION_HEADER_LEN
        val uncompressedLen = getUncompressedLength(input.slice())

        alloc.buffer(lenWithHeader, lenWithHeader).use { output ->
            output.writeByte(Js5CompressionType.GZIP.ordinal)
            output.writeInt(len)
            output.writeInt(uncompressedLen)
            output.writeBytes(input)

            return if (output.crc32() == expectedChecksum) {
                output.retain()
            } else {
                null
            }
        }
    }

    private fun groupToFile(input: ByteBuf, expectedChecksum: Int): ByteBuf? {
        val type = Js5CompressionType.fromOrdinal(input.readUnsignedByte().toInt())
        if (type != Js5CompressionType.GZIP) {
            return null
        }

        input.skipBytes(JS5_COMPRESSION_HEADER_LEN - 1)

        return if (input.crc32() == expectedChecksum) {
            input.retainedSlice()
        } else {
            null
        }
    }

    private companion object {
        private val OLD_TO_NEW_ENGINE = mapOf(
            1 to 7, // MODELS
            3 to 6, // MIDI_SONGS
            4 to 5, // MAPS
        )

        private const val JS5_COMPRESSION_HEADER_LEN = 9
    }
}
