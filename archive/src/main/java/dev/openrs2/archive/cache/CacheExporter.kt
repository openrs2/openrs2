package dev.openrs2.archive.cache

import dev.openrs2.buffer.use
import dev.openrs2.cache.Store
import dev.openrs2.db.Database
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class CacheExporter @Inject constructor(
    private val database: Database,
    private val alloc: ByteBufAllocator
) {
    public suspend fun export(id: Long, store: Store) {
        // TODO(gpe): think about what to do if there is a collision
        database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT 255::uint1, ci.archive_id::INTEGER, c.data, NULL
                FROM cache_indexes ci
                JOIN containers c ON c.id = ci.container_id
                WHERE ci.cache_id = ?
                UNION ALL
                SELECT ci.archive_id, ig.group_id, c.data, g.truncated_version
                FROM cache_indexes ci
                JOIN index_groups ig ON ig.container_id = ci.container_id
                JOIN groups g ON g.archive_id = ci.archive_id AND g.group_id = ig.group_id AND g.truncated_version = ig.version & 65535
                JOIN containers c ON c.id = g.container_id AND c.crc32 = ig.crc32
                WHERE ci.cache_id = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.fetchSize = BATCH_SIZE
                stmt.setLong(1, id)
                stmt.setLong(2, id)

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
                            }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        private val BATCH_SIZE = 1024
    }
}
