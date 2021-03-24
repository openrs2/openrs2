package org.openrs2.archive.cache

import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
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
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class CacheImporter @Inject constructor(
    private val database: Database,
    private val alloc: ByteBufAllocator
) {
    public abstract class Container(
        private val compressed: ByteBuf,
        private val uncompressed: ByteBuf?
    ) {
        public val bytes: ByteArray =
            ByteBufUtil.getBytes(compressed, compressed.readerIndex(), compressed.readableBytes(), false)
        public val crc32: Int = compressed.crc32()
        public val whirlpool: ByteArray = Whirlpool.whirlpool(bytes)
        public val encrypted: Boolean = uncompressed == null
        public val uncompressedLen: Int? = uncompressed?.readableBytes()
        public val uncompressedCrc32: Int? = uncompressed?.crc32()
        public val emptyLoc: Boolean = Js5Compression.isEmptyLoc(compressed.slice())

        public fun release() {
            compressed.release()
            uncompressed?.release()
        }
    }

    private class MasterIndex(
        val index: Js5MasterIndex,
        compressed: ByteBuf,
        uncompressed: ByteBuf
    ) : Container(compressed, uncompressed)

    public class Index(
        archive: Int,
        public val index: Js5Index,
        compressed: ByteBuf,
        uncompressed: ByteBuf
    ) : Group(Js5Archive.ARCHIVESET, archive, compressed, uncompressed, index.version, false)

    public open class Group(
        public val archive: Int,
        public val group: Int,
        compressed: ByteBuf,
        uncompressed: ByteBuf?,
        public val version: Int,
        public val versionTruncated: Boolean
    ) : Container(compressed, uncompressed)

    private enum class SourceType {
        DISK,
        JS5REMOTE
    }

    public data class MasterIndexResult(
        val masterIndexId: Int,
        val sourceId: Int,
        val indexes: List<ByteBuf?>
    )

    public suspend fun import(
        store: Store,
        game: String,
        build: Int?,
        timestamp: Instant?,
        name: String?,
        description: String?,
        url: String?
    ) {
        database.execute { connection ->
            prepare(connection)

            val gameId = getGameId(connection, game)

            // import master index
            val masterIndex = createMasterIndex(store)
            val masterIndexId = try {
                addMasterIndex(connection, masterIndex)
            } finally {
                masterIndex.release()
            }

            // create source
            val sourceId = addSource(
                connection,
                SourceType.DISK,
                masterIndexId,
                gameId,
                build,
                timestamp,
                name,
                description,
                url
            )

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
                    addIndex(connection, sourceId, index)
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
                            addGroups(connection, sourceId, groups)

                            groups.forEach(Group::release)
                            groups.clear()
                        }
                    }
                }

                if (groups.isNotEmpty()) {
                    addGroups(connection, sourceId, groups)
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
        description: String?,
        url: String?
    ) {
        Js5Compression.uncompress(buf.slice()).use { uncompressed ->
            val masterIndex = MasterIndex(Js5MasterIndex.read(uncompressed.slice(), format), buf, uncompressed)

            database.execute { connection ->
                prepare(connection)

                val gameId = getGameId(connection, game)
                val masterIndexId = addMasterIndex(connection, masterIndex)
                addSource(connection, SourceType.DISK, masterIndexId, gameId, build, timestamp, name, description, url)
            }
        }
    }

    public suspend fun importMasterIndexAndGetIndexes(
        masterIndex: Js5MasterIndex,
        buf: ByteBuf,
        uncompressed: ByteBuf,
        gameId: Int,
        build: Int,
        lastId: Int?,
        timestamp: Instant
    ): MasterIndexResult {
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

            val masterIndexId = addMasterIndex(
                connection,
                MasterIndex(masterIndex, buf, uncompressed)
            )

            val sourceId = addSource(
                connection,
                SourceType.JS5REMOTE,
                masterIndexId,
                gameId,
                build,
                timestamp,
                name = "Original",
                description = null,
                url = null
            )

            /*
             * In order to defend against (crc32, version) collisions, we only
             * use a cached index if its checksum/version haven't changed
             * between the previously downloaded version of the cache and the
             * current version. This emulates the behaviour of a client always
             * using the latest version of the cache - so if there is a
             * collision, real players of the game would experience problems.
             */
            connection.prepareStatement(
                """
                SELECT c.data
                FROM master_index_archives a
                LEFT JOIN master_index_archives a2 ON a2.master_index_id = ? AND a2.archive_id = a.archive_id AND
                    a2.crc32 = a.crc32 AND a2.version = a.version
                LEFT JOIN resolve_index(a2.master_index_id, a2.archive_id, a2.crc32, a2.version) c ON TRUE
                WHERE a.master_index_id = ?
                ORDER BY a.archive_id ASC
            """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, lastId, Types.INTEGER)
                stmt.setInt(2, masterIndexId)

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
                        return@execute MasterIndexResult(masterIndexId, sourceId, indexes)
                    } finally {
                        indexes.filterNotNull().forEach(ByteBuf::release)
                    }
                }
            }
        }
    }

    public suspend fun importIndexAndGetMissingGroups(
        sourceId: Int,
        archive: Int,
        index: Js5Index,
        buf: ByteBuf,
        uncompressed: ByteBuf,
        lastMasterIndexId: Int?
    ): List<Int> {
        return database.execute { connection ->
            prepare(connection)
            val id = addIndex(connection, sourceId, Index(archive, index, buf, uncompressed))

            /*
             * In order to defend against (crc32, version) collisions, we only
             * use a cached group if its checksum/version haven't changed
             * between the previously downloaded version of the cache and the
             * current version. This emulates the behaviour of a client always
             * using the latest version of the cache - so if there is a
             * collision, real players of the game would experience problems.
             *
             * We never use cached groups with a truncated version, as these
             * are even more likely to be prone to collisions.
             */
            connection.prepareStatement(
                """
                SELECT ig.group_id
                FROM index_groups ig
                LEFT JOIN resolved_indexes i ON i.master_index_id = ? AND
                    i.archive_id = ?
                LEFT JOIN index_groups ig2 ON ig2.container_id = i.container_id AND ig2.group_id = ig.group_id AND
                    ig2.crc32 = ig.crc32 AND ig2.version = ig.version
                LEFT JOIN resolve_group(i.master_index_id, i.archive_id, ig2.group_id, ig2.crc32, ig2.version) c ON TRUE
                WHERE ig.container_id = ? AND c.id IS NULL
                ORDER BY ig.group_id ASC
            """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, lastMasterIndexId, Types.INTEGER)
                stmt.setInt(2, archive)
                stmt.setLong(3, id)

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

    public suspend fun importGroups(sourceId: Int, groups: List<Group>) {
        if (groups.isEmpty()) {
            return
        }

        database.execute { connection ->
            prepare(connection)
            addGroups(connection, sourceId, groups)
        }
    }

    private fun createMasterIndex(store: Store): MasterIndex {
        val index = Js5MasterIndex.create(store)

        alloc.buffer().use { uncompressed ->
            index.write(uncompressed)

            Js5Compression.compress(uncompressed.slice(), Js5CompressionType.UNCOMPRESSED).use { buf ->
                return MasterIndex(index, buf.retain(), uncompressed.retain())
            }
        }
    }

    private fun addMasterIndex(
        connection: Connection,
        masterIndex: MasterIndex
    ): Int {
        val containerId = addContainer(connection, masterIndex)

        connection.prepareStatement(
            """
            SELECT id
            FROM master_indexes
            WHERE container_id = ? AND format = ?::master_index_format
        """.trimIndent()
        ).use { stmt ->
            stmt.setLong(1, containerId)
            stmt.setString(2, masterIndex.index.format.name.toLowerCase())

            stmt.executeQuery().use { rows ->
                if (rows.next()) {
                    return rows.getInt(1)
                }
            }
        }

        val masterIndexId: Int

        connection.prepareStatement(
            """
            INSERT INTO master_indexes (container_id, format)
            VALUES (?, ?::master_index_format)
            RETURNING id
        """.trimIndent()
        ).use { stmt ->
            stmt.setLong(1, containerId)
            stmt.setString(2, masterIndex.index.format.name.toLowerCase())

            stmt.executeQuery().use { rows ->
                check(rows.next())
                masterIndexId = rows.getInt(1)
            }
        }

        connection.prepareStatement(
            """
            INSERT INTO master_index_archives (
                master_index_id, archive_id, crc32, version, whirlpool, groups, total_uncompressed_length
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        ).use { stmt ->
            for ((i, entry) in masterIndex.index.entries.withIndex()) {
                stmt.setInt(1, masterIndexId)
                stmt.setInt(2, i)
                stmt.setInt(3, entry.checksum)

                if (masterIndex.index.format >= MasterIndexFormat.VERSIONED) {
                    stmt.setInt(4, entry.version)
                } else {
                    stmt.setInt(4, 0)
                }

                if (masterIndex.index.format >= MasterIndexFormat.DIGESTS) {
                    stmt.setBytes(5, entry.digest ?: ByteArray(Whirlpool.DIGESTBYTES))
                } else {
                    stmt.setNull(5, Types.BINARY)
                }

                if (masterIndex.index.format >= MasterIndexFormat.LENGTHS) {
                    stmt.setInt(6, entry.groups)
                    stmt.setInt(7, entry.totalUncompressedLength)
                } else {
                    stmt.setNull(6, Types.INTEGER)
                    stmt.setNull(7, Types.INTEGER)
                }

                stmt.addBatch()
            }

            stmt.executeBatch()
        }

        return masterIndexId
    }

    private fun addSource(
        connection: Connection,
        type: SourceType,
        masterIndexId: Int,
        gameId: Int,
        build: Int?,
        timestamp: Instant?,
        name: String?,
        description: String?,
        url: String?
    ): Int {
        if (type == SourceType.JS5REMOTE && build != null) {
            connection.prepareStatement(
                """
                SELECT id
                FROM sources
                WHERE type = 'js5remote' AND master_index_id = ? AND game_id = ? AND build = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, masterIndexId)
                stmt.setInt(2, gameId)
                stmt.setInt(3, build)

                stmt.executeQuery().use { rows ->
                    if (rows.next()) {
                        return rows.getInt(1)
                    }
                }
            }
        }

        connection.prepareStatement(
            """
            INSERT INTO sources (type, master_index_id, game_id, build, timestamp, name, description, url)
            VALUES (?::source_type, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, type.toString().toLowerCase())
            stmt.setInt(2, masterIndexId)
            stmt.setInt(3, gameId)
            stmt.setObject(4, build, Types.INTEGER)

            if (timestamp != null) {
                stmt.setObject(5, timestamp.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
            } else {
                stmt.setNull(5, Types.TIMESTAMP_WITH_TIMEZONE)
            }

            stmt.setString(6, name)
            stmt.setString(7, description)
            stmt.setString(8, url)

            stmt.executeQuery().use { rows ->
                check(rows.next())
                return rows.getInt(1)
            }
        }
    }

    private fun readGroup(store: Store, archive: Int, index: Js5Index?, group: Int): Group? {
        try {
            store.read(archive, group).use { buf ->
                var version = VersionTrailer.strip(buf) ?: return null
                var versionTruncated = true

                /*
                 * Grab the non-truncated version from the Js5Index if we can
                 * confirm the group on disk matches the group in the index.
                 */
                if (index != null) {
                    val entry = index[group]
                    if (entry != null && entry.checksum == buf.crc32() && (entry.version and 0xFFFF) == version) {
                        version = entry.version
                        versionTruncated = false
                    }
                }

                val slice = buf.slice()
                Js5Compression.uncompressUnlessEncrypted(slice).use { uncompressed ->
                    if (slice.isReadable) {
                        throw IOException("Trailing bytes after compressed data")
                    }

                    return Group(archive, group, buf.retain(), uncompressed?.retain(), version, versionTruncated)
                }
            }
        } catch (ex: IOException) {
            logger.warn(ex) { "Skipping corrupt group (archive $archive, group $group)" }
            return null
        }
    }

    private fun addGroups(connection: Connection, sourceId: Int, groups: List<Group>): List<Long> {
        val containerIds = addContainers(connection, groups)

        connection.prepareStatement(
            """
            INSERT INTO groups (archive_id, group_id, version, version_truncated, container_id)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT DO NOTHING
        """.trimIndent()
        ).use { stmt ->
            for ((i, group) in groups.withIndex()) {
                stmt.setInt(1, group.archive)
                stmt.setInt(2, group.group)
                stmt.setInt(3, group.version)
                stmt.setBoolean(4, group.versionTruncated)
                stmt.setLong(5, containerIds[i])
                stmt.addBatch()
            }

            stmt.executeBatch()
        }

        connection.prepareStatement(
            """
            INSERT INTO source_groups (source_id, archive_id, group_id, version, version_truncated, container_id)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT DO NOTHING
        """.trimIndent()
        ).use { stmt ->
            for ((i, group) in groups.withIndex()) {
                stmt.setInt(1, sourceId)
                stmt.setInt(2, group.archive)
                stmt.setInt(3, group.group)
                stmt.setInt(4, group.version)
                stmt.setBoolean(5, group.versionTruncated)
                stmt.setLong(6, containerIds[i])
                stmt.addBatch()
            }

            stmt.executeBatch()
        }

        return containerIds
    }

    private fun addGroup(connection: Connection, sourceId: Int, group: Group): Long {
        return addGroups(connection, sourceId, listOf(group)).single()
    }

    private fun readIndex(store: Store, archive: Int): Index {
        return store.read(Js5Archive.ARCHIVESET, archive).use { buf ->
            Js5Compression.uncompress(buf.slice()).use { uncompressed ->
                Index(archive, Js5Index.read(uncompressed.slice()), buf.retain(), uncompressed.retain())
            }
        }
    }

    private fun addIndex(connection: Connection, sourceId: Int, index: Index): Long {
        val containerId = addGroup(connection, sourceId, index)
        val savepoint = connection.setSavepoint()

        connection.prepareStatement(
            """
            INSERT INTO indexes (
                container_id, protocol, version, has_names, has_digests, has_lengths, has_uncompressed_checksums
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        ).use { stmt ->
            stmt.setLong(1, containerId)
            stmt.setInt(2, index.index.protocol.id)
            stmt.setInt(3, index.index.version)
            stmt.setBoolean(4, index.index.hasNames)
            stmt.setBoolean(5, index.index.hasDigests)
            stmt.setBoolean(6, index.index.hasLengths)
            stmt.setBoolean(7, index.index.hasUncompressedChecksums)

            try {
                stmt.execute()
            } catch (ex: SQLException) {
                if (ex.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
                    connection.rollback(savepoint)
                    return@addIndex containerId
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

        return containerId
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
                uncompressed_length INTEGER NULL,
                uncompressed_crc32 INTEGER NULL,
                data BYTEA NOT NULL,
                encrypted BOOLEAN NOT NULL,
                empty_loc BOOLEAN NULL
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
            INSERT INTO tmp_containers (index, crc32, whirlpool, data, uncompressed_length, uncompressed_crc32, encrypted, empty_loc)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        ).use { stmt ->
            for ((i, container) in containers.withIndex()) {
                stmt.setInt(1, i)
                stmt.setInt(2, container.crc32)
                stmt.setBytes(3, container.whirlpool)
                stmt.setBytes(4, container.bytes)
                stmt.setObject(5, container.uncompressedLen, Types.INTEGER)
                stmt.setObject(6, container.uncompressedCrc32, Types.INTEGER)
                stmt.setBoolean(7, container.encrypted)

                if (container.encrypted) {
                    stmt.setBoolean(8, container.emptyLoc)
                } else {
                    stmt.setNull(8, Types.BOOLEAN)
                }

                stmt.addBatch()
            }

            stmt.executeBatch()
        }

        connection.prepareStatement(
            """
            INSERT INTO containers (crc32, whirlpool, data, uncompressed_length, uncompressed_crc32, encrypted, empty_loc)
            SELECT t.crc32, t.whirlpool, t.data, t.uncompressed_length, t.uncompressed_crc32, t.encrypted, t.empty_loc
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

    public suspend fun setLastMasterIndexId(gameId: Int, masterIndexId: Int) {
        database.execute { connection ->
            connection.prepareStatement(
                """
                UPDATE games SET last_master_index_id = ? WHERE id = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, masterIndexId)
                stmt.setInt(2, gameId)

                stmt.execute()
            }
        }
    }

    public suspend fun refreshViews() {
        database.execute { connection ->
            connection.prepareStatement(
                """
                REFRESH MATERIALIZED VIEW CONCURRENTLY master_index_stats
            """.trimIndent()
            ).use { stmt ->
                stmt.execute()
            }
        }
    }

    public companion object {
        private val logger = InlineLogger()

        public const val BATCH_SIZE: Int = 1024
    }
}
