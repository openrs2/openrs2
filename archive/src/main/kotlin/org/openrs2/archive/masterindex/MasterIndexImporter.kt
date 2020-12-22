package org.openrs2.archive.masterindex

import io.netty.buffer.ByteBuf
import org.openrs2.archive.container.Container
import org.openrs2.archive.container.ContainerImporter
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.db.Database
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class MasterIndexImporter @Inject constructor(
    private val database: Database
) {
    private class MasterIndex(
        data: ByteBuf,
        val index: Js5MasterIndex
    ) : Container(data) {
        override val encrypted: Boolean = false
    }

    public suspend fun import(buf: ByteBuf) {
        database.execute { connection ->
            ContainerImporter.prepare(connection)

            val masterIndex = Js5Compression.uncompress(buf).use { uncompressed ->
                MasterIndex(buf, Js5MasterIndex.read(uncompressed))
            }

            val containerId = ContainerImporter.addContainer(connection, masterIndex)

            connection.prepareStatement(
                """
                INSERT INTO master_indexes (container_id)
                VALUES (?)
            """.trimIndent()
            ).use { stmt ->
                stmt.setLong(1, containerId)
                stmt.execute()
            }

            connection.prepareStatement(
                """
                INSERT INTO master_index_entries (container_id, archive_id, crc32, version)
                VALUES (?, ?, ?, ?)
            """.trimIndent()
            ).use { stmt ->
                for ((i, entry) in masterIndex.index.entries.withIndex()) {
                    stmt.setLong(1, containerId)
                    stmt.setInt(2, i)
                    stmt.setInt(3, entry.checksum)
                    stmt.setInt(4, entry.version)

                    stmt.addBatch()
                }

                stmt.executeBatch()
            }
        }
    }
}
