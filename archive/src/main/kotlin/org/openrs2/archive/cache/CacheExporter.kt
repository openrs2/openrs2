package org.openrs2.archive.cache

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Store
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
        val whirlpool: ByteArray,
        val game: String,
        val build: Int?,
        val timestamp: Instant?
    )

    public suspend fun list(): List<Cache> {
        return database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT c.id, c.whirlpool, g.name, m.build, m.timestamp
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
                        val whirlpool = rows.getBytes(2)
                        val game = rows.getString(3)

                        var build: Int? = rows.getInt(4)
                        if (rows.wasNull()) {
                            build = null
                        }

                        val timestamp = rows.getTimestamp(5)?.toInstant()

                        caches += Cache(id, whirlpool, game, build, timestamp)
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
                    JOIN master_index_archives a ON a.container_id = m.container_id
                    JOIN groups g ON g.archive_id = 255 AND g.group_id = a.archive_id::INTEGER AND g.truncated_version = a.version & 65535
                    JOIN containers c ON c.id = g.container_id AND c.crc32 = a.crc32
                    JOIN indexes i ON i.container_id = g.container_id AND i.version = a.version
                    WHERE m.container_id = ?
                )
                SELECT 255::uint1, t.archive_id::INTEGER, t.data, NULL
                FROM t
                UNION ALL
                SELECT t.archive_id, ig.group_id, c.data, g.truncated_version
                FROM t
                JOIN index_groups ig ON ig.container_id = t.container_id
                JOIN groups g ON g.archive_id = t.archive_id::INTEGER AND g.group_id = ig.group_id AND g.truncated_version = ig.version & 65535
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

    private companion object {
        private const val BATCH_SIZE = 1024
    }
}
