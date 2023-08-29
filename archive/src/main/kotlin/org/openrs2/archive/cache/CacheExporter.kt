package org.openrs2.archive.cache

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.buffer.use
import org.openrs2.cache.ChecksumTable
import org.openrs2.cache.DiskStore
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.cache.MasterIndexFormat
import org.openrs2.cache.Store
import org.openrs2.crypto.XteaKey
import org.openrs2.db.Database
import org.postgresql.util.PGobject
import java.sql.Connection
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.SortedSet

@Singleton
public class CacheExporter @Inject constructor(
    private val database: Database,
    private val alloc: ByteBufAllocator
) {
    public data class Stats(
        val validIndexes: Long,
        val indexes: Long,
        val validGroups: Long,
        val groups: Long,
        val validKeys: Long,
        val keys: Long,
        val size: Long,
        val blocks: Long
    ) {
        @JsonIgnore
        public val allIndexesValid: Boolean = indexes == validIndexes && indexes != 0L

        @JsonIgnore
        public val validIndexesFraction: Double = if (indexes == 0L) {
            1.0
        } else {
            validIndexes.toDouble() / indexes
        }

        @JsonIgnore
        public val allGroupsValid: Boolean = groups == validGroups

        @JsonIgnore
        public val validGroupsFraction: Double = if (groups == 0L) {
            1.0
        } else {
            validGroups.toDouble() / groups
        }

        @JsonIgnore
        public val allKeysValid: Boolean = keys == validKeys

        @JsonIgnore
        public val validKeysFraction: Double = if (keys == 0L) {
            1.0
        } else {
            validKeys.toDouble() / keys
        }

        /*
         * The max block ID is conveniently also the max number of blocks, as
         * zero is reserved.
         */
        public val diskStoreValid: Boolean = blocks <= DiskStore.MAX_BLOCK
    }

    public data class Archive(
        val resolved: Boolean,
        val stats: ArchiveStats?
    )

    public data class ArchiveStats(
        val validGroups: Long,
        val groups: Long,
        val validKeys: Long,
        val keys: Long,
        val size: Long,
        val blocks: Long
    ) {
        public val allGroupsValid: Boolean = groups == validGroups

        public val validGroupsFraction: Double = if (groups == 0L) {
            1.0
        } else {
            validGroups.toDouble() / groups
        }

        public val allKeysValid: Boolean = keys == validKeys

        public val validKeysFraction: Double = if (keys == 0L) {
            1.0
        } else {
            validKeys.toDouble() / keys
        }
    }

    public data class IndexStats(
        val validFiles: Long,
        val files: Long,
        val size: Long,
        val blocks: Long
    ) {
        public val allFilesValid: Boolean = files == validFiles

        public val validFilesFraction: Double = if (files == 0L) {
            1.0
        } else {
            validFiles.toDouble() / files
        }
    }

    public data class Build(val major: Int, val minor: Int?) : Comparable<Build> {
        override fun compareTo(other: Build): Int {
            return compareValuesBy(this, other, Build::major, Build::minor)
        }

        override fun toString(): String {
            return if (minor != null) {
                "$major.$minor"
            } else {
                major.toString()
            }
        }

        internal companion object {
            internal fun fromPgObject(o: PGobject): Build? {
                val value = o.value!!
                require(value.length >= 2)

                val parts = value.substring(1, value.length - 1).split(",")
                require(parts.size == 2)

                val major = parts[0]
                val minor = parts[1]

                if (major.isEmpty()) {
                    return null
                }

                return Build(major.toInt(), if (minor.isEmpty()) null else minor.toInt())
            }
        }
    }

    public data class CacheSummary(
        val id: Int,
        val scope: String,
        val game: String,
        val environment: String,
        val language: String,
        val builds: SortedSet<Build>,
        val timestamp: Instant?,
        val sources: SortedSet<String>,
        @JsonUnwrapped
        val stats: Stats?
    )

    public data class Cache(
        val id: Int,
        val sources: List<Source>,
        val updates: List<String>,
        val stats: Stats?,
        val archives: List<Archive>,
        val indexes: List<IndexStats>?,
        val masterIndex: Js5MasterIndex?,
        val checksumTable: ChecksumTable?
    )

    public data class Source(
        val game: String,
        val environment: String,
        val language: String,
        val build: Build?,
        val timestamp: Instant?,
        val name: String?,
        val description: String?,
        val url: String?
    )

    public data class Key(
        val archive: Int,
        val group: Int,
        val nameHash: Int?,
        val name: String?,
        @JsonProperty("mapsquare") val mapSquare: Int?,
        val key: XteaKey
    )

    public suspend fun totalSize(): Long {
        return database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT SUM(size)
                FROM cache_stats
                """.trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rows ->
                    if (rows.next()) {
                        rows.getLong(1)
                    } else {
                        0
                    }
                }
            }
        }
    }

    public suspend fun list(): List<CacheSummary> {
        return database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT *
                FROM (
                    SELECT
                        c.id,
                        g.name AS game,
                        sc.name AS scope,
                        e.name AS environment,
                        l.iso_code AS language,
                        array_remove(array_agg(DISTINCT ROW(s.build_major, s.build_minor)::build ORDER BY ROW(s.build_major, s.build_minor)::build ASC), NULL) builds,
                        MIN(s.timestamp) AS timestamp,
                        array_remove(array_agg(DISTINCT s.name ORDER BY s.name ASC), NULL) sources,
                        cs.valid_indexes,
                        cs.indexes,
                        cs.valid_groups,
                        cs.groups,
                        cs.valid_keys,
                        cs.keys,
                        cs.size,
                        cs.blocks
                    FROM caches c
                    JOIN sources s ON s.cache_id = c.id
                    JOIN game_variants v ON v.id = s.game_id
                    JOIN games g ON g.id = v.game_id
                    JOIN scopes sc ON sc.id = g.scope_id
                    JOIN environments e ON e.id = v.environment_id
                    JOIN languages l ON l.id = v.language_id
                    LEFT JOIN cache_stats cs ON cs.scope_id = sc.id AND cs.cache_id = c.id
                    WHERE NOT c.hidden
                    GROUP BY sc.name, c.id, g.name, e.name, l.iso_code, cs.valid_indexes, cs.indexes, cs.valid_groups,
                        cs.groups, cs.valid_keys, cs.keys, cs.size, cs.blocks
                ) t
                ORDER BY t.game ASC, t.environment ASC, t.language ASC, t.builds[1] ASC, t.timestamp ASC
                """.trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rows ->
                    val caches = mutableListOf<CacheSummary>()

                    while (rows.next()) {
                        val id = rows.getInt(1)
                        val game = rows.getString(2)
                        val scope = rows.getString(3)
                        val environment = rows.getString(4)
                        val language = rows.getString(5)
                        val builds = rows.getArray(6).array as Array<*>
                        val timestamp = rows.getTimestamp(7)?.toInstant()

                        @Suppress("UNCHECKED_CAST")
                        val sources = rows.getArray(8).array as Array<String>

                        val validIndexes = rows.getLong(9)
                        val stats = if (!rows.wasNull()) {
                            val indexes = rows.getLong(10)
                            val validGroups = rows.getLong(11)
                            val groups = rows.getLong(12)
                            val validKeys = rows.getLong(13)
                            val keys = rows.getLong(14)
                            val size = rows.getLong(15)
                            val blocks = rows.getLong(16)
                            Stats(validIndexes, indexes, validGroups, groups, validKeys, keys, size, blocks)
                        } else {
                            null
                        }

                        caches += CacheSummary(
                            id,
                            scope,
                            game,
                            environment,
                            language,
                            builds.mapNotNull { o -> Build.fromPgObject(o as PGobject) }.toSortedSet(),
                            timestamp,
                            sources.toSortedSet(),
                            stats
                        )
                    }

                    caches
                }
            }
        }
    }

    public suspend fun get(scope: String, id: Int): Cache? {
        return database.execute { connection ->
            val masterIndex: Js5MasterIndex?
            val checksumTable: ChecksumTable?
            val stats: Stats?

            connection.prepareStatement(
                """
                SELECT
                    m.format,
                    mc.data,
                    b.data,
                    cs.valid_indexes,
                    cs.indexes,
                    cs.valid_groups,
                    cs.groups,
                    cs.valid_keys,
                    cs.keys,
                    cs.size,
                    cs.blocks
                FROM caches c
                CROSS JOIN scopes s
                LEFT JOIN master_indexes m ON m.id = c.id
                LEFT JOIN containers mc ON mc.id = m.container_id
                LEFT JOIN crc_tables t ON t.id = c.id
                LEFT JOIN blobs b ON b.id = t.blob_id
                LEFT JOIN cache_stats cs ON cs.scope_id = s.id AND cs.cache_id = c.id
                WHERE s.name = ? AND c.id = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, scope)
                stmt.setInt(2, id)

                stmt.executeQuery().use { rows ->
                    if (!rows.next()) {
                        return@execute null
                    }

                    val formatString = rows.getString(1)
                    masterIndex = if (formatString != null) {
                        Unpooled.wrappedBuffer(rows.getBytes(2)).use { compressed ->
                            Js5Compression.uncompress(compressed).use { uncompressed ->
                                val format = MasterIndexFormat.valueOf(formatString.uppercase())
                                Js5MasterIndex.readUnverified(uncompressed, format)
                            }
                        }
                    } else {
                        null
                    }

                    val blob = rows.getBytes(3)
                    checksumTable = if (blob != null) {
                        Unpooled.wrappedBuffer(blob).use { buf ->
                            ChecksumTable.read(buf)
                        }
                    } else {
                        null
                    }

                    val validIndexes = rows.getLong(4)
                    stats = if (rows.wasNull()) {
                        null
                    } else {
                        val indexes = rows.getLong(5)
                        val validGroups = rows.getLong(6)
                        val groups = rows.getLong(7)
                        val validKeys = rows.getLong(8)
                        val keys = rows.getLong(9)
                        val size = rows.getLong(10)
                        val blocks = rows.getLong(11)
                        Stats(validIndexes, indexes, validGroups, groups, validKeys, keys, size, blocks)
                    }
                }
            }

            val sources = mutableListOf<Source>()

            connection.prepareStatement(
                """
                SELECT g.name, e.name, l.iso_code, s.build_major, s.build_minor, s.timestamp, s.name, s.description, s.url
                FROM sources s
                JOIN game_variants v ON v.id = s.game_id
                JOIN games g ON g.id = v.game_id
                JOIN scopes sc ON sc.id = g.scope_id
                JOIN environments e ON e.id = v.environment_id
                JOIN languages l ON l.id = v.language_id
                WHERE sc.name = ? AND s.cache_id = ?
                ORDER BY s.name ASC
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, scope)
                stmt.setInt(2, id)

                stmt.executeQuery().use { rows ->
                    while (rows.next()) {
                        val game = rows.getString(1)
                        val environment = rows.getString(2)
                        val language = rows.getString(3)

                        var buildMajor: Int? = rows.getInt(4)
                        if (rows.wasNull()) {
                            buildMajor = null
                        }

                        var buildMinor: Int? = rows.getInt(5)
                        if (rows.wasNull()) {
                            buildMinor = null
                        }

                        val build = if (buildMajor != null) {
                            Build(buildMajor, buildMinor)
                        } else {
                            null
                        }

                        val timestamp = rows.getTimestamp(6)?.toInstant()
                        val name = rows.getString(7)
                        val description = rows.getString(8)
                        val url = rows.getString(9)

                        sources += Source(game, environment, language, build, timestamp, name, description, url)
                    }
                }
            }

            val updates = mutableListOf<String>()

            connection.prepareStatement(
                """
                SELECT url
                FROM updates
                WHERE cache_id = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, id)

                stmt.executeQuery().use { rows ->
                    while (rows.next()) {
                        updates += rows.getString(1)
                    }
                }
            }

            val archives = mutableListOf<Archive>()

            connection.prepareStatement(
                """
                SELECT a.archive_id, c.id IS NOT NULL, s.valid_groups, s.groups, s.valid_keys, s.keys, s.size, s.blocks
                FROM master_index_archives a
                LEFT JOIN resolve_index((SELECT id FROM scopes WHERE name = ?), a.archive_id, a.crc32, a.version) c ON TRUE
                LEFT JOIN index_stats s ON s.container_id = c.id
                WHERE a.master_index_id = ?
                UNION ALL
                SELECT a.archive_id, b.id IS NOT NULL, NULL, NULL, NULL, NULL, length(b.data), group_blocks(a.archive_id, length(b.data))
                FROM crc_table_archives a
                LEFT JOIN resolve_archive(a.archive_id, a.crc32) b ON TRUE
                WHERE a.crc_table_id = ?
                ORDER BY archive_id ASC
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, scope)
                stmt.setInt(2, id)
                stmt.setInt(3, id)

                stmt.executeQuery().use { rows ->
                    while (rows.next()) {
                        val resolved = rows.getBoolean(2)

                        val size = rows.getLong(7)
                        val archiveStats = if (!rows.wasNull()) {
                            val validGroups = rows.getLong(3)
                            val groups = rows.getLong(4)
                            val validKeys = rows.getLong(5)
                            val keys = rows.getLong(6)
                            val blocks = rows.getLong(8)
                            ArchiveStats(validGroups, groups, validKeys, keys, size, blocks)
                        } else {
                            null
                        }

                        archives += Archive(resolved, archiveStats)
                    }
                }
            }

            val indexes = if (checksumTable != null && archives[5].resolved) {
                connection.prepareStatement(
                    """
                    SELECT s.valid_files, s.files, s.size, s.blocks
                    FROM crc_table_archives a
                    JOIN resolve_archive(a.archive_id, a.crc32) b ON TRUE
                    JOIN version_list_stats s ON s.blob_id = b.id
                    WHERE a.crc_table_id = ? AND a.archive_id = 5
                    ORDER BY s.index_id ASC
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setInt(1, id)

                    stmt.executeQuery().use { rows ->
                        val indexes = mutableListOf<IndexStats>()

                        while (rows.next()) {
                            val validFiles = rows.getLong(1)
                            val files = rows.getLong(2)
                            val size = rows.getLong(3)
                            val blocks = rows.getLong(4)

                            indexes += IndexStats(validFiles, files, size, blocks)
                        }

                        indexes
                    }
                }
            } else {
                null
            }

            Cache(id, sources, updates, stats, archives, indexes, masterIndex, checksumTable)
        }
    }

    public suspend fun getFileName(scope: String, id: Int): String? {
        return database.execute { connection ->
            // TODO(gpe): what if a cache is from multiple games?
            connection.prepareStatement(
                """
                SELECT
                    g.name AS game,
                    e.name AS environment,
                    l.iso_code AS language,
                    array_remove(array_agg(DISTINCT ROW(s.build_major, s.build_minor)::build ORDER BY ROW(s.build_major, s.build_minor)::build ASC), NULL) builds,
                    MIN(s.timestamp) AS timestamp
                FROM sources s
                JOIN game_variants v ON v.id = s.game_id
                JOIN games g ON g.id = v.game_id
                JOIN scopes sc ON sc.id = g.scope_id
                JOIN environments e ON e.id = v.environment_id
                JOIN languages l ON l.id = v.language_id
                WHERE sc.name = ? AND s.cache_id = ?
                GROUP BY g.name, e.name, l.iso_code
                LIMIT 1
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, scope)
                stmt.setInt(2, id)

                stmt.executeQuery().use { rows ->
                    if (!rows.next()) {
                        return@execute null
                    }

                    val game = rows.getString(1)
                    val environment = rows.getString(2)
                    val language = rows.getString(3)

                    val name = StringBuilder("$game-$environment-$language")

                    val builds = rows.getArray(4).array as Array<*>
                    for (build in builds.mapNotNull { o -> Build.fromPgObject(o as PGobject) }.toSortedSet()) {
                        name.append("-b")
                        name.append(build)
                    }

                    val timestamp = rows.getTimestamp(5)
                    if (!rows.wasNull()) {
                        name.append('-')
                        name.append(
                            timestamp.toInstant()
                                .atOffset(ZoneOffset.UTC)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"))
                        )
                    }

                    name.append("-openrs2#")
                    name.append(id)

                    name.toString()
                }
            }
        }
    }

    public suspend fun exportGroup(scope: String, id: Int, archive: Int, group: Int): ByteBuf? {
        return database.execute { connection ->
            if (archive == Store.ARCHIVESET && group == Store.ARCHIVESET) {
                connection.prepareStatement(
                    """
                    SELECT c.data
                    FROM master_indexes m
                    JOIN containers c ON c.id = m.container_id
                    WHERE m.id = ?
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setInt(1, id)

                    stmt.executeQuery().use { rows ->
                        if (rows.next()) {
                            val data = rows.getBytes(1)
                            return@execute Unpooled.wrappedBuffer(data)
                        }
                    }
                }
            }

            connection.prepareStatement(
                """
                SELECT g.data
                FROM resolved_groups g
                JOIN scopes s ON s.id = g.scope_id
                WHERE s.name = ? AND g.master_index_id = ? AND g.archive_id = ? AND g.group_id = ?
                UNION ALL
                SELECT f.data
                FROM resolved_files f
                WHERE f.crc_table_id = ? AND f.index_id = ? AND f.file_id = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, scope)
                stmt.setInt(2, id)
                stmt.setInt(3, archive)
                stmt.setInt(4, group)
                stmt.setInt(5, id)
                stmt.setInt(6, archive)
                stmt.setInt(7, group)

                stmt.executeQuery().use { rows ->
                    if (!rows.next()) {
                        return@execute null
                    }

                    val data = rows.getBytes(1)

                    return@execute Unpooled.wrappedBuffer(data)
                }
            }
        }
    }

    public fun export(scope: String, id: Int, storeFactory: (Boolean) -> Store) {
        database.executeOnce { connection ->
            val legacy = connection.prepareStatement(
                """
                SELECT id
                FROM crc_tables
                WHERE id = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, id)

                stmt.executeQuery().use { rows ->
                    rows.next()
                }
            }

            storeFactory(legacy).use { store ->
                if (legacy) {
                    exportLegacy(connection, id, store)
                } else {
                    export(connection, scope, id, store)
                }
            }
        }
    }

    private fun export(connection: Connection, scope: String, id: Int, store: Store) {
        connection.prepareStatement(
            """
            SELECT g.archive_id, g.group_id, g.data, g.version
            FROM resolved_groups g
            JOIN scopes s ON s.id = g.scope_id
            WHERE s.name = ? AND g.master_index_id = ?
            """.trimIndent()
        ).use { stmt ->
            stmt.fetchSize = BATCH_SIZE
            stmt.setString(1, scope)
            stmt.setInt(2, id)

            stmt.executeQuery().use { rows ->
                alloc.buffer(2, 2).use { versionBuf ->
                    store.create(Js5Archive.ARCHIVESET)

                    while (rows.next()) {
                        val archive = rows.getInt(1)
                        val group = rows.getInt(2)
                        val bytes = rows.getBytes(3)
                        val version = rows.getInt(4)
                        val versionNull = rows.wasNull()

                        versionBuf.clear()
                        if (!versionNull) {
                            versionBuf.writeShort(version)
                        }

                        Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(bytes), versionBuf.retain()).use { buf ->
                            store.write(archive, group, buf)

                            // ensure the .idx file exists even if it is empty
                            if (archive == Js5Archive.ARCHIVESET) {
                                store.create(group)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun exportLegacy(connection: Connection, id: Int, store: Store) {
        connection.prepareStatement(
            """
            SELECT index_id, file_id, data, version
            FROM resolved_files
            WHERE crc_table_id = ?
            """.trimIndent()
        ).use { stmt ->
            stmt.fetchSize = BATCH_SIZE
            stmt.setInt(1, id)

            stmt.executeQuery().use { rows ->
                alloc.buffer(2, 2).use { versionBuf ->
                    store.create(0)

                    while (rows.next()) {
                        val index = rows.getInt(1)
                        val file = rows.getInt(2)
                        val bytes = rows.getBytes(3)
                        val version = rows.getInt(4)
                        val versionNull = rows.wasNull()

                        versionBuf.clear()
                        if (!versionNull) {
                            versionBuf.writeShort(version)
                        }

                        Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(bytes), versionBuf.retain()).use { buf ->
                            store.write(index, file, buf)
                        }
                    }
                }
            }
        }
    }

    public suspend fun exportKeys(scope: String, id: Int): List<Key> {
        return database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT g.archive_id, g.group_id, g.name_hash, n.name, (k.key).k0, (k.key).k1, (k.key).k2, (k.key).k3
                FROM resolved_groups g
                JOIN scopes s ON s.id = g.scope_id
                JOIN keys k ON k.id = g.key_id
                LEFT JOIN names n ON n.hash = g.name_hash AND n.name ~ '^l(?:[0-9]|[1-9][0-9])_(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$'
                WHERE s.name = ? AND g.master_index_id = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, scope)
                stmt.setInt(2, id)

                stmt.executeQuery().use { rows ->
                    val keys = mutableListOf<Key>()

                    while (rows.next()) {
                        val archive = rows.getInt(1)
                        val group = rows.getInt(2)
                        var nameHash: Int? = rows.getInt(3)
                        if (rows.wasNull()) {
                            nameHash = null
                        }
                        val name = rows.getString(4)

                        val k0 = rows.getInt(5)
                        val k1 = rows.getInt(6)
                        val k2 = rows.getInt(7)
                        val k3 = rows.getInt(8)

                        val mapSquare = getMapSquare(name)
                        keys += Key(archive, group, nameHash, name, mapSquare, XteaKey(k0, k1, k2, k3))
                    }

                    keys
                }
            }
        }
    }

    private companion object {
        private const val BATCH_SIZE = 256
        private val LOC_NAME_REGEX = Regex("l(\\d+)_(\\d+)")

        private fun getMapSquare(name: String?): Int? {
            if (name == null) {
                return null
            }

            val match = LOC_NAME_REGEX.matchEntire(name) ?: return null
            val x = match.groupValues[1].toInt()
            val z = match.groupValues[2].toInt()
            return (x shl 8) or z
        }
    }
}
