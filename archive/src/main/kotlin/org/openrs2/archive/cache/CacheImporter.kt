package org.openrs2.archive.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.DefaultByteBufHolder
import io.netty.buffer.Unpooled
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5CompressionType
import org.openrs2.cache.Js5Index
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.cache.MasterIndexFormat
import org.openrs2.cache.Store
import org.openrs2.cache.VersionTrailer
import org.openrs2.crypto.Whirlpool
import org.openrs2.db.Database
import org.postgresql.util.PSQLState
import java.io.IOException
import java.sql.Connection
import java.sql.SQLException
import java.sql.Types
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class CacheImporter @Inject constructor(
    private val database: Database,
    private val alloc: ByteBufAllocator
) {
    public abstract class Container(
        data: ByteBuf,
        public val encrypted: Boolean
    ) : DefaultByteBufHolder(data) {
        public val bytes: ByteArray = ByteBufUtil.getBytes(data, data.readerIndex(), data.readableBytes(), false)
        public val crc32: Int = data.crc32()
        public val whirlpool: ByteArray = Whirlpool.whirlpool(bytes)
    }

    private class MasterIndex(
        val index: Js5MasterIndex,
        data: ByteBuf,
    ) : Container(data, false)

    public class Index(
        archive: Int,
        public val index: Js5Index,
        data: ByteBuf,
    ) : Group(Js5Archive.ARCHIVESET, archive, data, index.version, false, false)

    public open class Group(
        public val archive: Int,
        public val group: Int,
        data: ByteBuf,
        public val version: Int,
        public val versionTruncated: Boolean,
        encrypted: Boolean
    ) : Container(data, encrypted)

    public suspend fun import(
        store: Store,
        masterIndexFormat: MasterIndexFormat?,
        game: String,
        build: Int?,
        timestamp: Instant?,
        name: String?,
        description: String?
    ) {
        database.execute { connection ->
            prepare(connection)

            val gameId = getGameId(connection, game)

            // import master index
            val masterIndex = createMasterIndex(store, masterIndexFormat)
            try {
                addMasterIndex(connection, masterIndex, gameId, build, timestamp, name, description, false)
            } finally {
                masterIndex.release()
            }

            // import indexes
            val indexes = arrayOfNulls<Js5Index>(Js5Archive.ARCHIVESET)
            val indexGroups = mutableListOf<Index>()
            try {
                for (archive in store.list(Js5Archive.ARCHIVESET)) {
                    val indexGroup = readIndex(store, archive)
                    indexes[archive] = indexGroup.index
                    indexGroups += indexGroup
                }

                for (index in indexGroups) {
                    addIndex(connection, index)
                }
            } finally {
                indexGroups.forEach(Index::release)
            }

            // import groups
            val groups = mutableListOf<Group>()
            try {
                for (archive in store.list()) {
                    if (archive == Js5Archive.ARCHIVESET) {
                        continue
                    }

                    val index = indexes[archive]

                    for (id in store.list(archive)) {
                        val group = readGroup(store, archive, index, id) ?: continue
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

    public suspend fun importMasterIndex(
        buf: ByteBuf,
        format: MasterIndexFormat,
        game: String,
        build: Int?,
        timestamp: Instant?,
        name: String?,
        description: String?
    ) {
        Js5Compression.uncompress(buf.slice()).use { uncompressed ->
            val masterIndex = MasterIndex(Js5MasterIndex.read(uncompressed.slice(), format), buf)

            database.execute { connection ->
                prepare(connection)

                val gameId = getGameId(connection, game)
                addMasterIndex(connection, masterIndex, gameId, build, timestamp, name, description, false)
            }
        }
    }

    public suspend fun importMasterIndexAndGetIndexes(
        masterIndex: Js5MasterIndex,
        buf: ByteBuf,
        gameId: Int,
        build: Int,
        timestamp: Instant,
        name: String,
    ): List<ByteBuf?> {
        return database.execute { connection ->
            prepare(connection)

            connection.prepareStatement(
                """
                UPDATE games
                SET build = ?
                WHERE id = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, build)
                stmt.setInt(2, gameId)

                stmt.execute()
            }

            addMasterIndex(connection, MasterIndex(masterIndex, buf), gameId, build, timestamp, name, null, true)

            connection.prepareStatement(
                """
                CREATE TEMPORARY TABLE tmp_indexes (
                    archive_id uint1 NOT NULL,
                    crc32 INTEGER NOT NULL,
                    version INTEGER NOT NULL
                ) ON COMMIT DROP
            """.trimIndent()
            ).use { stmt ->
                stmt.execute()
            }

            connection.prepareStatement(
                """
                INSERT INTO tmp_indexes (archive_id, crc32, version)
                VALUES (?, ?, ?)
            """.trimIndent()
            ).use { stmt ->
                for ((i, entry) in masterIndex.entries.withIndex()) {
                    stmt.setInt(1, i)
                    stmt.setInt(2, entry.checksum)
                    stmt.setInt(3, entry.version)

                    stmt.addBatch()
                }

                stmt.executeBatch()
            }

            connection.prepareStatement(
                """
                SELECT c.data
                FROM tmp_indexes t
                LEFT JOIN containers c ON c.crc32 = t.crc32
                LEFT JOIN indexes i ON i.version = t.version AND i.container_id = c.id
                ORDER BY t.archive_id ASC
            """.trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rows ->
                    val indexes = mutableListOf<ByteBuf?>()
                    try {
                        while (rows.next()) {
                            val bytes = rows.getBytes(1)
                            if (bytes != null) {
                                indexes += Unpooled.wrappedBuffer(bytes)
                            } else {
                                indexes += null
                            }
                        }

                        indexes.filterNotNull().forEach(ByteBuf::retain)
                        return@execute indexes
                    } finally {
                        indexes.filterNotNull().forEach(ByteBuf::release)
                    }
                }
            }
        }
    }

    public suspend fun importIndexAndGetMissingGroups(archive: Int, index: Js5Index, buf: ByteBuf): List<Int> {
        return database.execute { connection ->
            prepare(connection)
            addIndex(connection, Index(archive, index, buf))

            connection.prepareStatement(
                """
                CREATE TEMPORARY TABLE tmp_groups (
                    group_id INTEGER NOT NULL,
                    crc32 INTEGER NOT NULL,
                    version INTEGER NOT NULL
                ) ON COMMIT DROP
            """.trimIndent()
            ).use { stmt ->
                stmt.execute()
            }

            connection.prepareStatement(
                """
                INSERT INTO tmp_groups (group_id, crc32, version)
                VALUES (?, ?, ?)
            """.trimIndent()
            ).use { stmt ->
                for (entry in index) {
                    stmt.setInt(1, entry.id)
                    stmt.setInt(2, entry.checksum)
                    stmt.setInt(3, entry.version)

                    stmt.addBatch()
                }

                stmt.executeBatch()
            }

            // we deliberately ignore groups with truncated versions here and
            // re-download them, just in case there's a (crc32, truncated version)
            // collision
            connection.prepareStatement(
                """
                SELECT t.group_id
                FROM tmp_groups t
                LEFT JOIN groups g ON g.archive_id = ? AND g.group_id = t.group_id AND g.version = t.version AND
                    NOT g.version_truncated
                LEFT JOIN containers c ON c.id = g.container_id AND c.crc32 = t.crc32
                WHERE g.container_id IS NULL
                ORDER BY t.group_id ASC
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, archive)

                stmt.executeQuery().use { rows ->
                    val groups = mutableListOf<Int>()

                    while (rows.next()) {
                        groups += rows.getInt(1)
                    }

                    return@execute groups
                }
            }
        }
    }

    public suspend fun importGroups(groups: List<Group>) {
        if (groups.isEmpty()) {
            return
        }

        database.execute { connection ->
            prepare(connection)
            addGroups(connection, groups)
        }
    }

    private fun createMasterIndex(store: Store, format: MasterIndexFormat?): MasterIndex {
        val index = Js5MasterIndex.create(store)

        alloc.buffer().use { uncompressed ->
            index.write(uncompressed, format ?: index.minimumFormat)

            Js5Compression.compress(uncompressed, Js5CompressionType.UNCOMPRESSED).use { buf ->
                return MasterIndex(index, buf.retain())
            }
        }
    }

    private fun addMasterIndex(
        connection: Connection,
        masterIndex: MasterIndex,
        gameId: Int,
        build: Int?,
        timestamp: Instant?,
        name: String?,
        description: String?,
        overwrite: Boolean
    ) {
        val containerId = addContainer(connection, masterIndex)
        var exists: Boolean

        var newBuild: Int?
        var newTimestamp: Instant?
        var newName: String?
        var newDescription: String?

        connection.prepareStatement(
            """
            SELECT game_id, build, timestamp, name, description
            FROM master_indexes
            WHERE container_id = ?
            FOR UPDATE
        """.trimIndent()
        ).use { stmt ->
            stmt.setLong(1, containerId)

            stmt.executeQuery().use { rows ->
                exists = rows.next()

                if (exists && !overwrite) {
                    val oldGameId = rows.getInt(1)

                    var oldBuild: Int? = rows.getInt(2)
                    if (rows.wasNull()) {
                        oldBuild = null
                    }

                    val oldTimestamp: Instant? = rows.getTimestamp(3)?.toInstant()
                    val oldName: String? = rows.getString(4)
                    val oldDescription: String? = rows.getString(5)

                    check(oldGameId == gameId)

                    if (oldBuild != null && build != null) {
                        check(oldBuild == build)
                        newBuild = oldBuild
                    } else if (oldBuild != null) {
                        newBuild = oldBuild
                    } else {
                        newBuild = build
                    }

                    if (oldTimestamp != null && timestamp != null) {
                        newTimestamp = if (oldTimestamp.isBefore(timestamp)) {
                            oldTimestamp
                        } else {
                            timestamp
                        }
                    } else if (oldTimestamp != null) {
                        newTimestamp = oldTimestamp
                    } else {
                        newTimestamp = timestamp
                    }

                    if (oldName != null && name != null) {
                        newName = "$oldName/$name"
                    } else if (oldName != null) {
                        newName = oldName
                    } else {
                        newName = name
                    }

                    if (oldDescription != null && description != null) {
                        newDescription = "$oldDescription\n\n$description"
                    } else if (oldDescription != null) {
                        newDescription = oldDescription
                    } else {
                        newDescription = description
                    }
                } else {
                    newBuild = build
                    newTimestamp = timestamp
                    newName = name
                    newDescription = description
                }
            }
        }

        connection.prepareStatement(
            """
            INSERT INTO master_indexes (container_id, game_id, build, timestamp, name, description)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (container_id) DO UPDATE SET
                game_id = EXCLUDED.game_id,
                build = EXCLUDED.build,
                timestamp = EXCLUDED.timestamp,
                name = EXCLUDED.name,
                description = EXCLUDED.description
        """.trimIndent()
        ).use { stmt ->
            stmt.setLong(1, containerId)
            stmt.setInt(2, gameId)
            stmt.setObject(3, newBuild, Types.INTEGER)

            if (newTimestamp != null) {
                val offsetDateTime = OffsetDateTime.ofInstant(newTimestamp, ZoneOffset.UTC)
                stmt.setObject(4, offsetDateTime, Types.TIMESTAMP_WITH_TIMEZONE)
            } else {
                stmt.setNull(4, Types.TIMESTAMP_WITH_TIMEZONE)
            }

            stmt.setString(5, newName)
            stmt.setString(6, newDescription)

            stmt.execute()
        }

        if (exists) {
            return
        }

        connection.prepareStatement(
            """
            INSERT INTO master_index_archives (container_id, archive_id, crc32, version)
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

    private fun readGroup(store: Store, archive: Int, index: Js5Index?, group: Int): Group? {
        try {
            store.read(archive, group).use { buf ->
                var version = VersionTrailer.strip(buf) ?: return null
                var versionTruncated = true
                val encrypted = Js5Compression.isEncrypted(buf.slice())

                // grab the non-truncated version from the Js5Index if we can
                // confirm the group on disk matches the group in the index
                if (index != null) {
                    val entry = index[group]
                    if (entry != null && entry.checksum == buf.crc32() && (entry.version and 0xFFFF) == version) {
                        version = entry.version
                        versionTruncated = false
                    }
                }

                return Group(archive, group, buf.retain(), version, versionTruncated, encrypted)
            }
        } catch (ex: IOException) {
            return null
        }
    }

    private fun addGroups(connection: Connection, groups: List<Group>): List<Long> {
        val containerIds = addContainers(connection, groups)

        connection.prepareStatement(
            """
            INSERT INTO groups (archive_id, group_id, container_id, version, version_truncated)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT DO NOTHING
        """.trimIndent()
        ).use { stmt ->
            for ((i, group) in groups.withIndex()) {
                stmt.setInt(1, group.archive)
                stmt.setInt(2, group.group)
                stmt.setLong(3, containerIds[i])
                stmt.setInt(4, group.version)
                stmt.setBoolean(5, group.versionTruncated)
                stmt.addBatch()
            }

            stmt.executeBatch()
        }

        return containerIds
    }

    private fun addGroup(connection: Connection, group: Group): Long {
        return addGroups(connection, listOf(group)).single()
    }

    private fun readIndex(store: Store, archive: Int): Index {
        return store.read(Js5Archive.ARCHIVESET, archive).use { buf ->
            Js5Compression.uncompress(buf.slice()).use { uncompressed ->
                Index(archive, Js5Index.read(uncompressed), buf.retain())
            }
        }
    }

    private fun addIndex(connection: Connection, index: Index) {
        val containerId = addGroup(connection, index)
        val savepoint = connection.setSavepoint()

        connection.prepareStatement(
            """
            INSERT INTO indexes (container_id)
            VALUES (?)
        """.trimIndent()
        ).use { stmt ->
            stmt.setLong(1, containerId)

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
            INSERT INTO index_groups (
                container_id, group_id, crc32, whirlpool, version, name_hash, length, uncompressed_length,
                uncompressed_crc32
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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

                if (index.index.hasLengths) {
                    stmt.setInt(7, group.length)
                    stmt.setInt(8, group.uncompressedLength)
                } else {
                    stmt.setNull(7, Types.INTEGER)
                    stmt.setNull(8, Types.INTEGER)
                }

                if (index.index.hasUncompressedChecksums) {
                    stmt.setInt(9, group.uncompressedChecksum)
                } else {
                    stmt.setNull(9, Types.INTEGER)
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

    private fun getGameId(connection: Connection, name: String): Int {
        connection.prepareStatement(
            """
                SELECT id
                FROM games
                WHERE name = ?
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, name)

            stmt.executeQuery().use { rows ->
                if (!rows.next()) {
                    throw Exception("Game not found")
                }

                return rows.getInt(1)
            }
        }
    }

    public companion object {
        public const val BATCH_SIZE: Int = 1024
    }
}
