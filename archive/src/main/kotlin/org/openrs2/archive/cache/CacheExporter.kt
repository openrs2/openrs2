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
    public data class Cache(
        val id: Long,
        val game: String,
        val build: Int?,
        val timestamp: Instant?,
        val name: String?
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
                SELECT m.id, g.name, m.build, m.timestamp, m.name
                FROM master_indexes m
                JOIN games g ON g.id = m.game_id
                JOIN containers c ON c.id = m.container_id
                ORDER BY g.name ASC, m.build ASC, m.timestamp ASC
            """.trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rows ->
                    val caches = mutableListOf<Cache>()

                    while (rows.next()) {
                        val id = rows.getLong(1)
                        val game = rows.getString(2)

                        var build: Int? = rows.getInt(3)
                        if (rows.wasNull()) {
                            build = null
                        }

                        val timestamp = rows.getTimestamp(4)?.toInstant()
                        val name = rows.getString(5)

                        caches += Cache(id, game, build, timestamp, name)
                    }

                    caches
                }
            }
        }
    }

    public suspend fun export(id: Long, store: Store) {
        // TODO(gpe): think about what to do if there is a collision
        database.execute { connection ->
            connection.prepareStatement(
                """
                WITH t AS (
                    SELECT a.archive_id, c.data, g.container_id
                    FROM master_indexes m
                    JOIN master_index_archives a ON a.master_index_id = m.id
                    JOIN groups g ON g.archive_id = 255 AND g.group_id = a.archive_id::INTEGER
                        AND g.version = a.version AND NOT g.version_truncated
                    JOIN containers c ON c.id = g.container_id AND c.crc32 = a.crc32
                    JOIN indexes i ON i.container_id = g.container_id AND i.version = a.version
                    WHERE m.id = ?
                )
                SELECT 255::uint1, t.archive_id::INTEGER, t.data, NULL
                FROM t
                UNION ALL
                SELECT t.archive_id, ig.group_id, c.data, g.version
                FROM t
                JOIN index_groups ig ON ig.container_id = t.container_id
                JOIN groups g ON g.archive_id = t.archive_id::INTEGER AND g.group_id = ig.group_id AND (
                    (g.version = ig.version AND NOT g.version_truncated) OR
                    (g.version = ig.version & 65535 AND g.version_truncated)
                )
                JOIN containers c ON c.id = g.container_id AND c.crc32 = ig.crc32
            """.trimIndent()
            ).use { stmt ->
                stmt.fetchSize = BATCH_SIZE
                stmt.setLong(1, id)

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

    public suspend fun exportKeys(id: Long): List<Key> {
        return database.execute { connection ->
            connection.prepareStatement(
                """
                WITH t AS (
                    SELECT a.archive_id, c.data, g.container_id
                    FROM master_indexes m
                    JOIN master_index_archives a ON a.master_index_id = m.id
                    JOIN groups g ON g.archive_id = 255 AND g.group_id = a.archive_id::INTEGER
                        AND g.version = a.version AND NOT g.version_truncated
                    JOIN containers c ON c.id = g.container_id AND c.crc32 = a.crc32
                    JOIN indexes i ON i.container_id = g.container_id AND i.version = a.version
                    WHERE m.id = ?
                )
                SELECT t.archive_id, ig.group_id, ig.name_hash, n.name, (k.key).k0, (k.key).k1, (k.key).k2, (k.key).k3
                FROM t
                JOIN index_groups ig ON ig.container_id = t.container_id
                JOIN groups g ON g.archive_id = t.archive_id::INTEGER AND g.group_id = ig.group_id AND (
                    (g.version = ig.version AND NOT g.version_truncated) OR
                    (g.version = ig.version & 65535 AND g.version_truncated)
                )
                JOIN containers c ON c.id = g.container_id AND c.crc32 = ig.crc32
                JOIN keys k ON k.id = c.key_id
                LEFT JOIN names n ON n.hash = ig.name_hash AND n.name ~ '^l(?:[0-9]|[1-9][0-9])_(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$'
            """.trimIndent()
            ).use { stmt ->
                stmt.setLong(1, id)

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
