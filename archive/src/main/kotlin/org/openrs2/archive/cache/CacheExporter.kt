package org.openrs2.archive.cache

import com.fasterxml.jackson.annotation.JsonProperty
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.cache.MasterIndexFormat
import org.openrs2.cache.Store
import org.openrs2.crypto.XteaKey
import org.openrs2.db.Database
import org.postgresql.util.PGobject
import java.time.Instant
import java.util.SortedSet
import javax.inject.Inject
import javax.inject.Singleton

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
        val size: Long
    ) {
        public val allIndexesValid: Boolean = indexes == validIndexes && indexes != 0L
        public val validIndexesFraction: Double = if (indexes == 0L) {
            1.0
        } else {
            validIndexes.toDouble() / indexes
        }

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
        val game: String,
        val builds: SortedSet<Build>,
        val timestamp: Instant?,
        val names: SortedSet<String>,
        val stats: Stats?
    )

    public data class Cache(
        val id: Int,
        val sources: List<Source>,
        val updates: List<String>,
        val stats: Stats?,
        val masterIndex: Js5MasterIndex
    )

    public data class Source(
        val game: String,
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

    public suspend fun list(): List<CacheSummary> {
        return database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT *
                FROM (
                    SELECT
                        m.id,
                        g.name,
                        array_remove(array_agg(DISTINCT ROW(s.build_major, s.build_minor)::build ORDER BY ROW(s.build_major, s.build_minor)::build ASC), NULL) builds,
                        MIN(s.timestamp) AS timestamp,
                        array_remove(array_agg(DISTINCT s.name ORDER BY s.name ASC), NULL) sources,
                        ms.valid_indexes,
                        ms.indexes,
                        ms.valid_groups,
                        ms.groups,
                        ms.valid_keys,
                        ms.keys,
                        ms.size
                    FROM master_indexes m
                    JOIN sources s ON s.master_index_id = m.id
                    JOIN games g ON g.id = s.game_id
                    LEFT JOIN master_index_stats ms ON ms.master_index_id = m.id
                    GROUP BY m.id, g.name, ms.valid_indexes, ms.indexes, ms.valid_groups, ms.groups, ms.valid_keys, ms.keys,
                        ms.size
                ) t
                ORDER BY t.name ASC, t.builds[1] ASC, t.timestamp ASC
            """.trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rows ->
                    val caches = mutableListOf<CacheSummary>()

                    while (rows.next()) {
                        val id = rows.getInt(1)
                        val game = rows.getString(2)
                        val builds = rows.getArray(3).array as Array<Any>
                        val timestamp = rows.getTimestamp(4)?.toInstant()
                        val names = rows.getArray(5).array as Array<String>

                        val validIndexes = rows.getLong(6)
                        val stats = if (!rows.wasNull()) {
                            val indexes = rows.getLong(7)
                            val validGroups = rows.getLong(8)
                            val groups = rows.getLong(9)
                            val validKeys = rows.getLong(10)
                            val keys = rows.getLong(11)
                            val size = rows.getLong(12)
                            Stats(validIndexes, indexes, validGroups, groups, validKeys, keys, size)
                        } else {
                            null
                        }

                        caches += CacheSummary(
                            id,
                            game,
                            builds.mapNotNull { o -> Build.fromPgObject(o as PGobject) }.toSortedSet(),
                            timestamp,
                            names.toSortedSet(),
                            stats
                        )
                    }

                    caches
                }
            }
        }
    }

    public suspend fun get(id: Int): Cache? {
        return database.execute { connection ->
            val masterIndex: Js5MasterIndex
            val stats: Stats?

            connection.prepareStatement(
                """
                SELECT
                    m.format,
                    c.data,
                    ms.valid_indexes,
                    ms.indexes,
                    ms.valid_groups,
                    ms.groups,
                    ms.valid_keys,
                    ms.keys,
                    ms.size
                FROM master_indexes m
                JOIN containers c ON c.id = m.container_id
                LEFT JOIN master_index_stats ms ON ms.master_index_id = m.id
                WHERE m.id = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, id)

                stmt.executeQuery().use { rows ->
                    if (!rows.next()) {
                        return@execute null
                    }

                    val format = MasterIndexFormat.valueOf(rows.getString(1).uppercase())

                    masterIndex = Unpooled.wrappedBuffer(rows.getBytes(2)).use { compressed ->
                        Js5Compression.uncompress(compressed).use { uncompressed ->
                            Js5MasterIndex.readUnverified(uncompressed, format)
                        }
                    }

                    val validIndexes = rows.getLong(3)
                    stats = if (rows.wasNull()) {
                        null
                    } else {
                        val indexes = rows.getLong(4)
                        val validGroups = rows.getLong(5)
                        val groups = rows.getLong(6)
                        val validKeys = rows.getLong(7)
                        val keys = rows.getLong(8)
                        val size = rows.getLong(9)
                        Stats(validIndexes, indexes, validGroups, groups, validKeys, keys, size)
                    }
                }
            }

            val sources = mutableListOf<Source>()

            connection.prepareStatement(
                """
                SELECT g.name, s.build_major, s.build_minor, s.timestamp, s.name, s.description, s.url
                FROM sources s
                JOIN games g ON g.id = s.game_id
                WHERE s.master_index_id = ?
                ORDER BY s.name ASC
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, id)

                stmt.executeQuery().use { rows ->
                    while (rows.next()) {
                        val game = rows.getString(1)

                        var buildMajor: Int? = rows.getInt(2)
                        if (rows.wasNull()) {
                            buildMajor = null
                        }

                        var buildMinor: Int? = rows.getInt(3)
                        if (rows.wasNull()) {
                            buildMinor = null
                        }

                        val build = if (buildMajor != null) {
                            Build(buildMajor, buildMinor)
                        } else {
                            null
                        }

                        val timestamp = rows.getTimestamp(4)?.toInstant()
                        val name = rows.getString(5)
                        val description = rows.getString(6)
                        val url = rows.getString(7)

                        sources += Source(game, build, timestamp, name, description, url)
                    }
                }
            }

            val updates = mutableListOf<String>()

            connection.prepareStatement(
                """
                SELECT url
                FROM updates
                WHERE master_index_id = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, id)

                stmt.executeQuery().use { rows ->
                    while (rows.next()) {
                        updates += rows.getString(1)
                    }
                }
            }

            Cache(id, sources, updates, stats, masterIndex)
        }
    }

    public suspend fun export(id: Int, store: Store) {
        database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT archive_id, group_id, data, version
                FROM resolved_groups
                WHERE master_index_id = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.fetchSize = BATCH_SIZE
                stmt.setInt(1, id)

                stmt.executeQuery().use { rows ->
                    alloc.buffer(2, 2).use { versionBuf ->
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
    }

    public suspend fun exportKeys(id: Int): List<Key> {
        return database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT g.archive_id, g.group_id, g.name_hash, n.name, (k.key).k0, (k.key).k1, (k.key).k2, (k.key).k3
                FROM resolved_groups g
                JOIN keys k ON k.id = g.key_id
                LEFT JOIN names n ON n.hash = g.name_hash AND n.name ~ '^l(?:[0-9]|[1-9][0-9])_(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$'
                WHERE g.master_index_id = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, id)

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
        private const val BATCH_SIZE = 1024
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
