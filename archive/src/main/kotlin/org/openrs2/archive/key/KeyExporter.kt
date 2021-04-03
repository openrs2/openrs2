package org.openrs2.archive.key

import org.openrs2.crypto.XteaKey
import org.openrs2.db.Database
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class KeyExporter @Inject constructor(
    private val database: Database
) {
    public data class Stats(
        val allKeys: Long,
        val validKeys: Long,
        val encryptedGroups: Long,
        val validGroups: Long
    )

    public suspend fun count(): Stats {
        return database.execute { connection ->
            val encryptedGroups: Long
            val validGroups: Long

            connection.prepareStatement(
                """
                SELECT
                    COUNT(*),
                    COUNT(*) FILTER (WHERE c.key_id IS NOT NULL)
                FROM containers c
                WHERE c.encrypted
            """.trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rows ->
                    check(rows.next())

                    encryptedGroups = rows.getLong(1)
                    validGroups = rows.getLong(2)
                }
            }

            connection.prepareStatement(
                """
                SELECT
                    COUNT(DISTINCT k.id),
                    COUNT(DISTINCT k.id) FILTER (WHERE c.key_id IS NOT NULL)
                FROM keys k
                LEFT JOIN containers c ON c.key_id = k.id
            """.trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rows ->
                    check(rows.next())

                    val allKeys = rows.getLong(1)
                    val validKeys = rows.getLong(2)
                    Stats(allKeys, validKeys, encryptedGroups, validGroups)
                }
            }
        }
    }

    public suspend fun exportAll(): List<XteaKey> {
        return export(validOnly = false)
    }

    public suspend fun exportValid(): List<XteaKey> {
        return export(validOnly = true)
    }

    private suspend fun export(validOnly: Boolean): List<XteaKey> {
        return database.execute { connection ->
            val query = if (validOnly) {
                EXPORT_VALID_QUERY
            } else {
                EXPORT_ALL_QUERY
            }

            connection.prepareStatement(query).use { stmt ->
                stmt.executeQuery().use { rows ->
                    val keys = mutableListOf<XteaKey>()

                    while (rows.next()) {
                        val k0 = rows.getInt(1)
                        val k1 = rows.getInt(2)
                        val k2 = rows.getInt(3)
                        val k3 = rows.getInt(4)
                        keys += XteaKey(k0, k1, k2, k3)
                    }

                    keys
                }
            }
        }
    }

    private companion object {
        private val EXPORT_ALL_QUERY = """
            SELECT (k.key).k0, (k.key).k1, (k.key).k2, (k.key).k3
            FROM keys k
            ORDER BY k.id ASC
        """.trimIndent()

        private val EXPORT_VALID_QUERY = """
            SELECT (k.key).k0, (k.key).k1, (k.key).k2, (k.key).k3
            FROM keys k
            JOIN containers c ON c.key_id = k.id
            ORDER BY k.id ASC
        """.trimIndent()
    }
}
