package org.openrs2.archive.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.DefaultByteBufHolder
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5CompressionType
import org.openrs2.cache.Js5Index
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.cache.Store
import org.openrs2.cache.VersionTrailer
import org.openrs2.crypto.Whirlpool
import org.openrs2.db.Database
import org.postgresql.util.PSQLState
import java.io.IOException
import java.sql.Connection
import java.sql.SQLException
import java.sql.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class CacheImporter @Inject constructor(
    private val database: Database,
    private val alloc: ByteBufAllocator
) {
    private abstract class Container(
        data: ByteBuf
    ) : DefaultByteBufHolder(data) {
        val bytes: ByteArray = ByteBufUtil.getBytes(data, data.readerIndex(), data.readableBytes(), false)
        val crc32: Int = data.crc32()
        val whirlpool: ByteArray = Whirlpool.whirlpool(bytes)
        abstract val encrypted: Boolean
    }

    private class MasterIndex(
        val index: Js5MasterIndex,
        data: ByteBuf
    ) : Container(data) {
        override val encrypted: Boolean = false
    }

    private class Index(
        val index: Js5Index,
        data: ByteBuf
    ) : Container(data) {
        override val encrypted: Boolean = false
    }

    private class Group(
        val archive: Int,
        val group: Int,
        data: ByteBuf,
        val version: Int,
        override val encrypted: Boolean
    ) : Container(data)

    public suspend fun import(store: Store) {
        database.execute { connection ->
            prepare(connection)

            // import master index
            val masterIndex = createMasterIndex(store)
            try {
                addMasterIndex(connection, masterIndex)
            } finally {
                masterIndex.release()
            }

            // import indexes
            val indexes = mutableListOf<Index>()
            try {
                for (archive in store.list(Js5Archive.ARCHIVESET)) {
                    indexes += readIndex(store, archive)
                }

                for (index in indexes) {
                    addIndex(connection, index)
                }
            } finally {
                indexes.forEach(Index::release)
            }

            // import groups
            val groups = mutableListOf<Group>()
            try {
                for (archive in store.list()) {
                    if (archive == Js5Archive.ARCHIVESET) {
                        continue
                    }

                    for (id in store.list(archive)) {
                        val group = readGroup(store, archive, id) ?: continue
                        groups += group

                        if (groups.size >= BATCH_SIZE) {
                            addGroups(connection, groups)

                            groups.forEach(Group::release)
                            groups.clear()
                        }
                    }
                }

                if (groups.isNotEmpty()) {
                    addGroups(connection, groups)
                }
            } finally {
                groups.forEach(Group::release)
            }
        }
    }

    public suspend fun importMasterIndex(buf: ByteBuf) {
        Js5Compression.uncompress(buf.slice()).use { uncompressed ->
            val masterIndex = MasterIndex(Js5MasterIndex.read(uncompressed.slice()), buf)

            database.execute { connection ->
                prepare(connection)
                addMasterIndex(connection, masterIndex)
            }
        }
    }

    private fun createMasterIndex(store: Store): MasterIndex {
        val index = Js5MasterIndex.create(store)

        alloc.buffer().use { uncompressed ->
            index.write(uncompressed)

            Js5Compression.compress(uncompressed, Js5CompressionType.UNCOMPRESSED).use { buf ->
                return MasterIndex(index, buf.retain())
            }
        }
    }

    private fun addMasterIndex(connection: Connection, masterIndex: MasterIndex) {
        val containerId = addContainer(connection, masterIndex)
        val savepoint = connection.setSavepoint()

        connection.prepareStatement(
            """
            INSERT INTO master_indexes (container_id)
            VALUES (?)
        """.trimIndent()
        ).use { stmt ->
            stmt.setLong(1, containerId)

            try {
                stmt.execute()
            } catch (ex: SQLException) {
                if (ex.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
                    connection.rollback(savepoint)
                    return@addMasterIndex
                }
                throw ex
            }
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

    private fun readGroup(store: Store, archive: Int, group: Int): Group? {
        try {
            store.read(archive, group).use { buf ->
                val version = VersionTrailer.strip(buf) ?: return null
                val encrypted = Js5Compression.isEncrypted(buf.slice())
                return Group(archive, group, buf.retain(), version, encrypted)
            }
        } catch (ex: IOException) {
            return null
        }
    }

    private fun addGroups(connection: Connection, groups: List<Group>) {
        val containerIds = addContainers(connection, groups)

        connection.prepareStatement(
            """
            INSERT INTO groups (archive_id, group_id, container_id, truncated_version)
            VALUES (?, ?, ?, ?)
            ON CONFLICT DO NOTHING
        """.trimIndent()
        ).use { stmt ->
            for ((i, group) in groups.withIndex()) {
                stmt.setInt(1, group.archive)
                stmt.setInt(2, group.group)
                stmt.setLong(3, containerIds[i])
                stmt.setInt(4, group.version)
                stmt.addBatch()
            }

            stmt.executeBatch()
        }
    }

    private fun readIndex(store: Store, archive: Int): Index {
        return store.read(Js5Archive.ARCHIVESET, archive).use { buf ->
            Js5Compression.uncompress(buf.slice()).use { uncompressed ->
                Index(Js5Index.read(uncompressed), buf.retain())
            }
        }
    }

    private fun addIndex(connection: Connection, index: Index) {
        val containerId = addContainer(connection, index)
        val savepoint = connection.setSavepoint()

        connection.prepareStatement(
            """
            INSERT INTO indexes (container_id, version)
            VALUES (?, ?)
        """.trimIndent()
        ).use { stmt ->
            stmt.setLong(1, containerId)
            stmt.setInt(2, index.index.version)

            try {
                stmt.execute()
            } catch (ex: SQLException) {
                if (ex.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
                    connection.rollback(savepoint)
                    return@addIndex
                }
                throw ex
            }
        }

        connection.prepareStatement(
            """
            INSERT INTO index_groups (container_id, group_id, crc32, whirlpool, version, name_hash)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()
        ).use { stmt ->
            for (group in index.index) {
                stmt.setLong(1, containerId)
                stmt.setInt(2, group.id)
                stmt.setInt(3, group.checksum)
                stmt.setBytes(4, group.digest)
                stmt.setInt(5, group.version)

                if (index.index.hasNames) {
                    stmt.setInt(6, group.nameHash)
                } else {
                    stmt.setNull(6, Types.INTEGER)
                }

                stmt.addBatch()
            }

            stmt.executeBatch()
        }

        connection.prepareStatement(
            """
            INSERT INTO index_files (container_id, group_id, file_id, name_hash)
            VALUES (?, ?, ?, ?)
        """.trimIndent()
        ).use { stmt ->
            for (group in index.index) {
                for (file in group) {
                    stmt.setLong(1, containerId)
                    stmt.setInt(2, group.id)
                    stmt.setInt(3, file.id)

                    if (index.index.hasNames) {
                        stmt.setInt(4, file.nameHash)
                    } else {
                        stmt.setNull(4, Types.INTEGER)
                    }

                    stmt.addBatch()
                }
            }

            stmt.executeBatch()
        }
    }

    private fun prepare(connection: Connection) {
        connection.prepareStatement(
            """
            LOCK TABLE containers IN EXCLUSIVE MODE
        """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        connection.prepareStatement(
            """
            CREATE TEMPORARY TABLE tmp_containers (
                index INTEGER NOT NULL,
                crc32 INTEGER NOT NULL,
                whirlpool BYTEA NOT NULL,
                data BYTEA NOT NULL,
                encrypted BOOLEAN NOT NULL
            ) ON COMMIT DROP
        """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }
    }

    private fun addContainer(connection: Connection, container: Container): Long {
        return addContainers(connection, listOf(container)).single()
    }

    private fun addContainers(connection: Connection, containers: List<Container>): List<Long> {
        connection.prepareStatement(
            """
            TRUNCATE TABLE tmp_containers
        """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        connection.prepareStatement(
            """
            INSERT INTO tmp_containers (index, crc32, whirlpool, data, encrypted)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        ).use { stmt ->
            for ((i, container) in containers.withIndex()) {
                stmt.setInt(1, i)
                stmt.setInt(2, container.crc32)
                stmt.setBytes(3, container.whirlpool)
                stmt.setBytes(4, container.bytes)
                stmt.setBoolean(5, container.encrypted)
                stmt.addBatch()
            }

            stmt.executeBatch()
        }

        connection.prepareStatement(
            """
            INSERT INTO containers (crc32, whirlpool, data, encrypted)
            SELECT t.crc32, t.whirlpool, t.data, t.encrypted
            FROM tmp_containers t
            LEFT JOIN containers c ON c.whirlpool = t.whirlpool
            WHERE c.whirlpool IS NULL
            ON CONFLICT DO NOTHING
        """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        val ids = mutableListOf<Long>()

        connection.prepareStatement(
            """
            SELECT c.id
            FROM tmp_containers t
            JOIN containers c ON c.whirlpool = t.whirlpool
            ORDER BY t.index ASC
        """.trimIndent()
        ).use { stmt ->
            stmt.executeQuery().use { rows ->
                while (rows.next()) {
                    ids += rows.getLong(1)
                }
            }
        }

        check(ids.size == containers.size)
        return ids
    }

    private companion object {
        private const val BATCH_SIZE = 1024
    }
}
