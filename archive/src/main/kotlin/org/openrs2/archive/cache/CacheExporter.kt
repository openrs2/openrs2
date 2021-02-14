package org.openrs2.archive.cache

import com.fasterxml.jackson.annotation.JsonProperty
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Store
import org.openrs2.crypto.XteaKey
import org.openrs2.db.Database
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class CacheExporter @Inject constructor(
    private val database: Database,
    private val alloc: ByteBufAllocator
) {
    public data class ArchiveStats(val indexes: Long, val validIndexes: Long) {
        public val allIndexesValid: Boolean = indexes == validIndexes
    }

    public data class GroupStats(val groups: Long, val validGroups: Long, val keys: Long, val validKeys: Long) {
        public val allGroupsValid: Boolean = groups == validGroups
        public val allKeysValid: Boolean = keys == validKeys
    }

    public data class Cache(
        val id: Int,
        val game: String,
        val build: Int?,
        val timestamp: Instant?,
        val name: String?,
        val archiveStats: ArchiveStats?,
        val groupStats: GroupStats?
    )

    public data class Key(
        val archive: Int,
        val group: Int,
        val nameHash: Int?,
        val name: String?,
        @JsonProperty("mapsquare") val mapSquare: Int?,
        val key: XteaKey
    )

    public suspend fun list(): List<Cache> {
        return database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT
                    m.id, g.name, m.build, m.timestamp, m.name,
                    a.indexes, a.valid_indexes, gs.groups, gs.valid_groups, gs.keys, gs.valid_keys
                FROM master_indexes m
                JOIN games g ON g.id = m.game_id
                JOIN containers c ON c.id = m.container_id
                LEFT JOIN master_index_archive_stats a ON a.master_index_id = m.id
                LEFT JOIN master_index_group_stats gs ON gs.master_index_id = m.id
                ORDER BY g.name ASC, m.build ASC, m.timestamp ASC
            """.trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rows ->
                    val caches = mutableListOf<Cache>()

                    while (rows.next()) {
                        val id = rows.getInt(1)
                        val game = rows.getString(2)

                        var build: Int? = rows.getInt(3)
                        if (rows.wasNull()) {
                            build = null
                        }

                        val timestamp = rows.getTimestamp(4)?.toInstant()
                        val name = rows.getString(5)

                        val indexes = rows.getLong(6)
                        val archiveStats = if (!rows.wasNull()) {
                            val validIndexes = rows.getLong(7)
                            ArchiveStats(indexes, validIndexes)
                        } else {
                            null
                        }

                        val groups = rows.getLong(8)
                        val groupStats = if (!rows.wasNull()) {
                            val validGroups = rows.getLong(9)
                            val keys = rows.getLong(10)
                            val validKeys = rows.getLong(11)
                            GroupStats(groups, validGroups, keys, validKeys)
                        } else {
                            null
                        }

                        caches += Cache(id, game, build, timestamp, name, archiveStats, groupStats)
                    }

                    caches
                }
            }
        }
    }

    public suspend fun export(id: Int, store: Store) {
        database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT archive_id, group_id, data, version
                FROM master_index_valid_groups
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
                SELECT v.archive_id, v.group_id, v.name_hash, n.name, (k.key).k0, (k.key).k1, (k.key).k2, (k.key).k3
                FROM master_index_valid_groups v
                JOIN keys k ON k.id = v.key_id
                LEFT JOIN names n ON n.hash = v.name_hash AND n.name ~ '^l(?:[0-9]|[1-9][0-9])_(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$'
                WHERE v.master_index_id = ?
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
