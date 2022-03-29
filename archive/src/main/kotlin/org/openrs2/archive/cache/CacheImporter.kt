package org.openrs2.archive.cache

import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.DefaultByteBufHolder
import io.netty.buffer.Unpooled
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use
import org.openrs2.cache.ChecksumTable
import org.openrs2.cache.DiskStore
import org.openrs2.cache.JagArchive
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5CompressionType
import org.openrs2.cache.Js5Index
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.cache.MasterIndexFormat
import org.openrs2.cache.Store
import org.openrs2.cache.StoreCorruptException
import org.openrs2.cache.VersionList
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

    public abstract class Blob(
        buf: ByteBuf
    ) : DefaultByteBufHolder(buf) {
        public val bytes: ByteArray = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false)
        public val crc32: Int = buf.crc32()
        public val whirlpool: ByteArray = Whirlpool.whirlpool(bytes)
    }

    public class ChecksumTableBlob(
        buf: ByteBuf,
        public val table: ChecksumTable
    ) : Blob(buf)

    public class Archive(
        public val id: Int,
        buf: ByteBuf,
        public val versionList: VersionList?
    ) : Blob(buf)

    public class File(
        public val index: Int,
        public val file: Int,
        buf: ByteBuf,
        public val version: Int
    ) : Blob(buf)

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
        environment: String,
        language: String,
        buildMajor: Int?,
        buildMinor: Int?,
        timestamp: Instant?,
        name: String?,
        description: String?,
        url: String?
    ) {
        database.execute { connection ->
            prepare(connection)

            val gameId = getGameId(connection, game, environment, language)

            if (store is DiskStore && store.legacy) {
                importLegacy(connection, store, gameId, buildMajor, buildMinor, timestamp, name, description, url)
            } else {
                importJs5(connection, store, gameId, buildMajor, buildMinor, timestamp, name, description, url)
            }
        }
    }

    private fun importJs5(
        connection: Connection,
        store: Store,
        gameId: Int,
        buildMajor: Int?,
        buildMinor: Int?,
        timestamp: Instant?,
        name: String?,
        description: String?,
        url: String?
    ) {
        // import master index
        val masterIndex = createMasterIndex(store)
        val masterIndexId = try {
            if (masterIndex.index.entries.isEmpty()) {
                throw IOException("Master index empty, cache probably corrupt")
            }

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
            buildMajor,
            buildMinor,
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
                try {
                    val indexGroup = readIndex(store, archive)
                    indexes[archive] = indexGroup.index
                    indexGroups += indexGroup
                } catch (ex: StoreCorruptException) {
                    // see the comment in Js5MasterIndex::create
                    logger.warn(ex) { "Skipping corrupt index (archive $archive)" }
                }
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

    public suspend fun importMasterIndex(
        buf: ByteBuf,
        format: MasterIndexFormat,
        game: String,
        environment: String,
        language: String,
        buildMajor: Int?,
        buildMinor: Int?,
        timestamp: Instant?,
        name: String?,
        description: String?,
        url: String?
    ) {
        Js5Compression.uncompress(buf.slice()).use { uncompressed ->
            val masterIndex = MasterIndex(
                Js5MasterIndex.readUnverified(uncompressed.slice(), format),
                buf,
                uncompressed
            )

            database.execute { connection ->
                prepare(connection)

                val gameId = getGameId(connection, game, environment, language)
                val masterIndexId = addMasterIndex(connection, masterIndex)

                addSource(
                    connection,
                    SourceType.DISK,
                    masterIndexId,
                    gameId,
                    buildMajor,
                    buildMinor,
                    timestamp,
                    name,
                    description,
                    url
                )
            }
        }
    }

    public suspend fun importMasterIndexAndGetIndexes(
        masterIndex: Js5MasterIndex,
        buf: ByteBuf,
        uncompressed: ByteBuf,
        gameId: Int,
        buildMajor: Int,
        buildMinor: Int?,
        lastId: Int?,
        timestamp: Instant
    ): CacheImporter.MasterIndexResult {
        return database.execute { connection ->
            prepare(connection)

            connection.prepareStatement(
                """
                UPDATE game_variants
                SET build_major = ?, build_minor = ?
                WHERE id = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, buildMajor)
                stmt.setObject(2, buildMinor, Types.INTEGER)
                stmt.setInt(3, gameId)

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
                buildMajor,
                buildMinor,
                timestamp,
                name = "Jagex",
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
                LEFT JOIN resolve_index(a2.archive_id, a2.crc32, a2.version) c ON TRUE
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
                LEFT JOIN resolve_group(i.archive_id, ig2.group_id, ig2.crc32, ig2.version) c ON TRUE
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
            stmt.setString(2, masterIndex.index.format.name.lowercase())

            stmt.executeQuery().use { rows ->
                if (rows.next()) {
                    return rows.getInt(1)
                }
            }
        }

        val masterIndexId: Int

        connection.prepareStatement(
            """
            INSERT INTO caches (id)
            VALUES (DEFAULT)
            RETURNING id
        """.trimIndent()
        ).use { stmt ->
            stmt.executeQuery().use { rows ->
                check(rows.next())
                masterIndexId = rows.getInt(1)
            }
        }

        connection.prepareStatement(
            """
            INSERT INTO master_indexes (id, container_id, format)
            VALUES (?, ?, ?::master_index_format)
        """.trimIndent()
        ).use { stmt ->
            stmt.setInt(1, masterIndexId)
            stmt.setLong(2, containerId)
            stmt.setString(3, masterIndex.index.format.name.lowercase())

            stmt.execute()
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
        cacheId: Int,
        gameId: Int,
        buildMajor: Int?,
        buildMinor: Int?,
        timestamp: Instant?,
        name: String?,
        description: String?,
        url: String?
    ): Int {
        if (type == SourceType.JS5REMOTE && buildMajor != null) {
            connection.prepareStatement(
                """
                SELECT id
                FROM sources
                WHERE type = 'js5remote' AND cache_id = ? AND game_id = ? AND build_major = ? AND build_minor IS NOT DISTINCT FROM ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, cacheId)
                stmt.setInt(2, gameId)
                stmt.setInt(3, buildMajor)
                stmt.setObject(4, buildMinor, Types.INTEGER)

                stmt.executeQuery().use { rows ->
                    if (rows.next()) {
                        return rows.getInt(1)
                    }
                }
            }
        }

        connection.prepareStatement(
            """
            INSERT INTO sources (type, cache_id, game_id, build_major, build_minor, timestamp, name, description, url)
            VALUES (?::source_type, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, type.toString().lowercase())
            stmt.setInt(2, cacheId)
            stmt.setInt(3, gameId)
            stmt.setObject(4, buildMajor, Types.INTEGER)
            stmt.setObject(5, buildMinor, Types.INTEGER)

            if (timestamp != null) {
                stmt.setObject(6, timestamp.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
            } else {
                stmt.setNull(6, Types.TIMESTAMP_WITH_TIMEZONE)
            }

            stmt.setString(7, name)
            stmt.setString(8, description)
            stmt.setString(9, url)

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

        connection.prepareStatement(
            """
            CREATE TEMPORARY TABLE tmp_blobs (
                index INTEGER NOT NULL,
                crc32 INTEGER NOT NULL,
                whirlpool BYTEA NOT NULL,
                data BYTEA NOT NULL
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

    private fun addBlob(connection: Connection, blob: Blob): Long {
        return addBlobs(connection, listOf(blob)).single()
    }

    private fun addBlobs(connection: Connection, blobs: List<Blob>): List<Long> {
        connection.prepareStatement(
            """
            TRUNCATE TABLE tmp_blobs
        """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        connection.prepareStatement(
            """
            INSERT INTO tmp_blobs (index, crc32, whirlpool, data)
            VALUES (?, ?, ?, ?)
        """.trimIndent()
        ).use { stmt ->
            for ((i, blob) in blobs.withIndex()) {
                stmt.setInt(1, i)
                stmt.setInt(2, blob.crc32)
                stmt.setBytes(3, blob.whirlpool)
                stmt.setBytes(4, blob.bytes)

                stmt.addBatch()
            }

            stmt.executeBatch()
        }

        connection.prepareStatement(
            """
            INSERT INTO blobs (crc32, whirlpool, data)
            SELECT t.crc32, t.whirlpool, t.data
            FROM tmp_blobs t
            LEFT JOIN blobs b ON b.whirlpool = t.whirlpool
            WHERE b.whirlpool IS NULL
            ON CONFLICT DO NOTHING
        """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        val ids = mutableListOf<Long>()

        connection.prepareStatement(
            """
            SELECT b.id
            FROM tmp_blobs t
            JOIN blobs b ON b.whirlpool = t.whirlpool
            ORDER BY t.index ASC
        """.trimIndent()
        ).use { stmt ->
            stmt.executeQuery().use { rows ->
                while (rows.next()) {
                    ids += rows.getLong(1)
                }
            }
        }

        check(ids.size == blobs.size)
        return ids
    }

    private fun getGameId(connection: Connection, name: String, environment: String, language: String): Int {
        connection.prepareStatement(
            """
                SELECT v.id
                FROM game_variants v
                JOIN games g ON g.id = v.game_id
                JOIN environments e ON e.id = v.environment_id
                JOIN languages l ON l.id = v.language_id
                WHERE g.name = ? AND e.name = ? AND l.iso_code = ?
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, name)
            stmt.setString(2, environment)
            stmt.setString(3, language)

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
                UPDATE game_variants SET last_master_index_id = ? WHERE id = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, masterIndexId)
                stmt.setInt(2, gameId)

                stmt.execute()
            }
        }
    }

    private fun importLegacy(
        connection: Connection,
        store: Store,
        gameId: Int,
        buildMajor: Int?,
        buildMinor: Int?,
        timestamp: Instant?,
        name: String?,
        description: String?,
        url: String?
    ) {
        // import checksum table
        val checksumTable = createChecksumTable(store)
        val checksumTableId = try {
            if (checksumTable.table.entries.isEmpty()) {
                throw IOException("Checksum table empty, cache probably corrupt")
            }

            addChecksumTable(connection, checksumTable)
        } finally {
            checksumTable.release()
        }

        // add source
        val sourceId = addSource(
            connection,
            SourceType.DISK,
            checksumTableId,
            gameId,
            buildMajor,
            buildMinor,
            timestamp,
            name,
            description,
            url
        )

        // import archives and version list
        for (id in store.list(0)) {
            try {
                readArchive(store, id).use { archive ->
                    addArchive(connection, sourceId, archive)
                }
            } catch (ex: StoreCorruptException) {
                // see the comment in ChecksumTable::create
                logger.warn(ex) { "Skipping corrupt archive ($id)" }
            }
        }

        // import files
        val files = mutableListOf<File>()
        try {
            for (index in store.list()) {
                if (index == 0) {
                    continue
                }

                for (id in store.list(index)) {
                    val file = readFile(store, index, id) ?: continue
                    files += file

                    if (files.size >= BATCH_SIZE) {
                        addFiles(connection, sourceId, files)

                        files.forEach(File::release)
                        files.clear()
                    }
                }
            }

            if (files.isNotEmpty()) {
                addFiles(connection, sourceId, files)
            }
        } finally {
            files.forEach(File::release)
        }
    }

    private fun createChecksumTable(store: Store): ChecksumTableBlob {
        alloc.buffer().use { buf ->
            val table = ChecksumTable.create(store)
            table.write(buf)
            return ChecksumTableBlob(buf.retain(), table)
        }
    }

    private fun addChecksumTable(
        connection: Connection,
        checksumTable: ChecksumTableBlob
    ): Int {
        val blobId = addBlob(connection, checksumTable)

        connection.prepareStatement(
            """
            SELECT id
            FROM crc_tables
            WHERE blob_id = ?
        """.trimIndent()
        ).use { stmt ->
            stmt.setLong(1, blobId)

            stmt.executeQuery().use { rows ->
                if (rows.next()) {
                    return rows.getInt(1)
                }
            }
        }

        val checksumTableId: Int

        connection.prepareStatement(
            """
            INSERT INTO caches (id)
            VALUES (DEFAULT)
            RETURNING id
        """.trimIndent()
        ).use { stmt ->
            stmt.executeQuery().use { rows ->
                check(rows.next())
                checksumTableId = rows.getInt(1)
            }
        }

        connection.prepareStatement(
            """
            INSERT INTO crc_tables (id, blob_id)
            VALUES (?, ?)
        """.trimIndent()
        ).use { stmt ->
            stmt.setInt(1, checksumTableId)
            stmt.setLong(2, blobId)

            stmt.execute()
        }

        connection.prepareStatement(
            """
            INSERT INTO crc_table_archives (crc_table_id, archive_id, crc32)
            VALUES (?, ?, ?)
        """.trimIndent()
        ).use { stmt ->
            for ((i, entry) in checksumTable.table.entries.withIndex()) {
                stmt.setInt(1, checksumTableId)
                stmt.setInt(2, i)
                stmt.setInt(3, entry)

                stmt.addBatch()
            }

            stmt.executeBatch()
        }

        return checksumTableId
    }

    private fun readArchive(store: Store, id: Int): Archive {
        store.read(0, id).use { buf ->
            val versionList = if (id == 5) {
                JagArchive.unpack(buf.slice()).use { archive ->
                    VersionList.read(archive)
                }
            } else {
                null
            }

            return Archive(id, buf.retain(), versionList)
        }
    }

    private fun addArchive(connection: Connection, sourceId: Int, archive: Archive) {
        val blobId = addBlob(connection, archive)

        connection.prepareStatement(
            """
            INSERT INTO archives (archive_id, blob_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
        """.trimIndent()
        ).use { stmt ->
            stmt.setInt(1, archive.id)
            stmt.setLong(2, blobId)

            stmt.execute()
        }

        connection.prepareStatement(
            """
            INSERT INTO source_archives (source_id, archive_id, blob_id)
            VALUES (?, ?, ?)
            ON CONFLICT DO NOTHING
        """.trimIndent()
        ).use { stmt ->
            stmt.setInt(1, sourceId)
            stmt.setInt(2, archive.id)
            stmt.setLong(3, blobId)

            stmt.execute()
        }

        val versionList = archive.versionList ?: return
        val savepoint = connection.setSavepoint()

        connection.prepareStatement(
            """
            INSERT INTO version_lists (blob_id)
            VALUES (?)
        """.trimIndent()
        ).use { stmt ->
            try {
                stmt.setLong(1, blobId)

                stmt.execute()
            } catch (ex: SQLException) {
                if (ex.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
                    connection.rollback(savepoint)
                    return
                }
                throw ex
            }
        }

        connection.prepareStatement(
            """
            INSERT INTO version_list_files (blob_id, index_id, file_id, version, crc32)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        ).use { stmt ->
            for ((indexId, files) in versionList.files.withIndex()) {
                for ((fileId, file) in files.withIndex()) {
                    if (file.version == 0) {
                        continue
                    }

                    stmt.setLong(1, blobId)
                    stmt.setInt(2, indexId + 1)
                    stmt.setInt(3, fileId)
                    stmt.setInt(4, file.version)
                    stmt.setInt(5, file.checksum)

                    stmt.addBatch()
                }
            }

            stmt.executeBatch()
        }

        connection.prepareStatement(
            """
            INSERT INTO version_list_maps (blob_id, map_square, map_file_id, loc_file_id, free_to_play)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        ).use { stmt ->
            for ((mapSquare, map) in versionList.maps) {
                stmt.setLong(1, blobId)
                stmt.setInt(2, mapSquare)
                stmt.setInt(3, map.mapFile)
                stmt.setInt(4, map.locFile)
                stmt.setBoolean(5, map.freeToPlay)

                stmt.addBatch()
            }

            stmt.executeBatch()
        }
    }

    private fun readFile(store: Store, index: Int, file: Int): File? {
        try {
            store.read(index, file).use { buf ->
                val version = VersionTrailer.strip(buf) ?: return null

                // TODO(gpe): try ungzipping here?

                return File(index, file, buf.retain(), version)
            }
        } catch (ex: IOException) {
            logger.warn(ex) { "Skipping corrupt file (index $index, group $file)" }
            return null
        }
    }

    private fun addFiles(connection: Connection, sourceId: Int, files: List<File>) {
        val blobIds = addBlobs(connection, files)

        connection.prepareStatement(
            """
            INSERT INTO files (index_id, file_id, version, blob_id)
            VALUES (?, ?, ?, ?)
            ON CONFLICT DO NOTHING
        """.trimIndent()
        ).use { stmt ->
            for ((i, file) in files.withIndex()) {
                stmt.setInt(1, file.index)
                stmt.setInt(2, file.file)
                stmt.setInt(3, file.version)
                stmt.setLong(4, blobIds[i])

                stmt.addBatch()
            }

            stmt.executeBatch()
        }

        connection.prepareStatement(
            """
            INSERT INTO source_files (source_id, index_id, file_id, version, blob_id)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT DO NOTHING
        """.trimIndent()
        ).use { stmt ->
            for ((i, file) in files.withIndex()) {
                stmt.setInt(1, sourceId)
                stmt.setInt(2, file.index)
                stmt.setInt(3, file.file)
                stmt.setInt(4, file.version)
                stmt.setLong(5, blobIds[i])

                stmt.addBatch()
            }

            stmt.executeBatch()
        }
    }

    public suspend fun refreshViews() {
        database.execute { connection ->
            connection.prepareStatement("""
                SELECT pg_try_advisory_lock(0)
            """.trimIndent()).use { stmt ->
                stmt.executeQuery().use { rows ->
                    if (!rows.next()) {
                        throw IllegalStateException()
                    }

                    val locked = rows.getBoolean(1)
                    if (!locked) {
                        return@execute
                    }
                }
            }

            connection.prepareStatement(
                """
                REFRESH MATERIALIZED VIEW CONCURRENTLY index_stats
            """.trimIndent()
            ).use { stmt ->
                stmt.execute()
            }

            connection.prepareStatement(
                """
                REFRESH MATERIALIZED VIEW CONCURRENTLY master_index_stats
            """.trimIndent()
            ).use { stmt ->
                stmt.execute()
            }

            connection.prepareStatement(
                """
                REFRESH MATERIALIZED VIEW CONCURRENTLY version_list_stats
            """.trimIndent()
            ).use { stmt ->
                stmt.execute()
            }

            connection.prepareStatement(
                """
                REFRESH MATERIALIZED VIEW CONCURRENTLY crc_table_stats
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
