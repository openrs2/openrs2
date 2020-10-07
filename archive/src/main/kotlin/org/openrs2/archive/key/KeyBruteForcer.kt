package org.openrs2.archive.key

import io.netty.buffer.Unpooled
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Compression
import org.openrs2.crypto.XteaKey
import org.openrs2.db.Database
import java.sql.Connection
import java.sql.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class KeyBruteForcer @Inject constructor(
    private val database: Database
) {
    /*
     * The code for writing to the containers and keys tables ensures that the
     * row IDs are allocated monotonically (by forbidding any other
     * transactions from writing simultaneously with an EXCLUSIVE table lock).
     *
     * Rather than storing a list of (container, key) pairs which have yet to
     * be tested (or have already been tested), which would take O(n*m) space,
     * the containers/keys are tested in order. This means we only need to
     * store the IDs of the last container/key tested.
     *
     * If the container/key combinations are represented as a matrix, it looks
     * like the diagram below:
     *
     *        containers ->
     *       +----------+----------+
     *     k |##########|          |
     *     e |##########|    A     |
     *     y |##########|          |
     *     s +----------+----------+
     *       |          |          |
     *     | |    C     |    B     |
     *     v |          |          |
     *       +----------+----------+
     *
     * The shaded are represents combinations that have already been tried.
     *
     * When a new container is inserted, we test it against every key in the
     * shaded area (quadrant A).
     *
     * When a new key is inserted, we test it against every container in the
     * shaded area (quadrant C).
     *
     * If keys and containers are inserted simultaneously, we take care to
     * avoid testing them twice (quadrant B) by testing new containers against
     * all keys but not vice-versa.
     *
     * This code can't tolerate new IDs being inserted while it runs, so it
     * locks the tables in SHARE mode. This prevents the import process from
     * running (which takes EXCLUSIVE locks) but allows other processes to read
     * from the tables.
     */
    public suspend fun bruteForce() {
        database.execute { connection ->
            connection.prepareStatement(
                """
                LOCK TABLE containers, keys IN SHARE MODE
            """.trimIndent()
            ).use { stmt ->
                stmt.execute()
            }

            bruteForceNewContainers(connection) // A, B
            bruteForceNewKeys(connection) // C
        }
    }

    private fun bruteForceNewContainers(connection: Connection) {
        var lastContainerId: Long?

        connection.prepareStatement(
            """
            SELECT last_container_id
            FROM brute_force_iterator
            FOR UPDATE
        """.trimIndent()
        ).use { stmt ->
            stmt.executeQuery().use { rows ->
                check(rows.next())

                lastContainerId = rows.getLong(1)
                if (rows.wasNull()) {
                    lastContainerId = null
                }
            }
        }

        while (true) {
            val pair = nextContainer(connection, lastContainerId) ?: break
            val (containerId, data) = pair

            connection.prepareStatement(
                """
                SELECT id, (key).k0, (key).k1, (key).k2, (key).k3
                FROM keys
            """.trimIndent()
            ).use { stmt ->
                stmt.fetchSize = BATCH_SIZE

                stmt.executeQuery().use { rows ->
                    while (rows.next()) {
                        val keyId = rows.getLong(1)

                        val k0 = rows.getInt(2)
                        val k1 = rows.getInt(3)
                        val k2 = rows.getInt(4)
                        val k3 = rows.getInt(5)
                        val key = XteaKey(k0, k1, k2, k3)

                        val valid = Unpooled.wrappedBuffer(data).use { buf ->
                            Js5Compression.isKeyValid(buf, key)
                        }

                        if (valid) {
                            connection.prepareStatement(
                                """
                                UPDATE containers
                                SET key_id = ?
                                WHERE id = ?
                            """.trimIndent()
                            ).use { stmt ->
                                stmt.setLong(1, keyId)
                                stmt.setLong(2, containerId)
                                stmt.execute()
                            }
                            break
                        }
                    }
                }

                lastContainerId = containerId
            }
        }

        connection.prepareStatement(
            """
            UPDATE brute_force_iterator
            SET last_container_id = ?
        """.trimIndent()
        ).use { stmt ->
            stmt.setObject(1, lastContainerId, Types.BIGINT)
            stmt.execute()
        }
    }

    private fun nextContainer(connection: Connection, lastContainerId: Long?): Pair<Long, ByteArray>? {
        connection.prepareStatement(
            """
            SELECT id, data
            FROM containers
            WHERE (? IS NULL OR id > ?) AND encrypted AND key_id IS NULL
            LIMIT 1
        """.trimIndent()
        ).use { stmt ->
            stmt.setObject(1, lastContainerId, Types.BIGINT)
            stmt.setObject(2, lastContainerId, Types.BIGINT)

            stmt.executeQuery().use { rows ->
                if (!rows.next()) {
                    return null
                }

                val containerId = rows.getLong(1)
                val data = rows.getBytes(2)
                return Pair(containerId, data)
            }
        }
    }

    private fun bruteForceNewKeys(connection: Connection) {
        var lastKeyId: Long?
        var lastContainerId: Long

        connection.prepareStatement(
            """
            SELECT last_key_id, last_container_id
            FROM brute_force_iterator
            FOR UPDATE
        """.trimIndent()
        ).use { stmt ->
            stmt.executeQuery().use { rows ->
                check(rows.next())

                lastKeyId = rows.getLong(1)
                if (rows.wasNull()) {
                    lastKeyId = null
                }

                lastContainerId = rows.getLong(2)
                if (rows.wasNull()) {
                    return@bruteForceNewKeys
                }
            }
        }

        while (true) {
            val pair = nextKey(connection, lastKeyId) ?: break
            val (keyId, key) = pair

            connection.prepareStatement(
                """
                SELECT id, data
                FROM containers
                WHERE encrypted AND key_id IS NULL AND id < ?
            """.trimIndent()
            ).use { stmt ->
                stmt.fetchSize = BATCH_SIZE
                stmt.setLong(1, lastContainerId)

                stmt.executeQuery().use { rows ->
                    while (rows.next()) {
                        val containerId = rows.getLong(1)
                        val data = rows.getBytes(2)

                        val valid = Unpooled.wrappedBuffer(data).use { buf ->
                            Js5Compression.isKeyValid(buf, key)
                        }

                        if (valid) {
                            connection.prepareStatement(
                                """
                                UPDATE containers
                                SET key_id = ?
                                WHERE id = ?
                            """.trimIndent()
                            ).use { stmt ->
                                stmt.setLong(1, keyId)
                                stmt.setLong(2, containerId)
                                stmt.execute()
                            }
                        }
                    }
                }
            }

            lastKeyId = keyId
        }

        connection.prepareStatement(
            """
            UPDATE brute_force_iterator
            SET last_key_id = ?
        """.trimIndent()
        ).use { stmt ->
            stmt.setObject(1, lastKeyId, Types.BIGINT)
            stmt.execute()
        }
    }

    private fun nextKey(connection: Connection, lastKeyId: Long?): Pair<Long, XteaKey>? {
        connection.prepareStatement(
            """
            SELECT id, (key).k0, (key).k1, (key).k2, (key).k3
            FROM keys
            WHERE ? IS NULL OR id > ?
            LIMIT 1
        """.trimIndent()
        ).use { stmt ->
            stmt.setObject(1, lastKeyId, Types.BIGINT)
            stmt.setObject(2, lastKeyId, Types.BIGINT)

            stmt.executeQuery().use { rows ->
                if (!rows.next()) {
                    return null
                }

                val keyId = rows.getLong(1)

                val k0 = rows.getInt(2)
                val k1 = rows.getInt(3)
                val k2 = rows.getInt(4)
                val k3 = rows.getInt(5)
                val key = XteaKey(k0, k1, k2, k3)

                return Pair(keyId, key)
            }
        }
    }

    private companion object {
        private const val BATCH_SIZE = 1024
    }
}
