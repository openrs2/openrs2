package org.openrs2.archive.key

import org.openrs2.crypto.XteaKey
import org.openrs2.db.Database
import java.io.BufferedOutputStream
import java.io.DataOutputStream
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
        val validGroups: Long,
        val emptyGroups: Long
    ) {
        val validKeysFraction: Double = if (allKeys == 0L) {
            1.0
        } else {
            validKeys.toDouble() / allKeys
        }

        val validGroupsFraction: Double = if (encryptedGroups == 0L) {
            1.0
        } else {
            validGroups.toDouble() / encryptedGroups
        }

        val emptyGroupsFraction: Double = if (encryptedGroups == 0L) {
            1.0
        } else {
            emptyGroups.toDouble() / encryptedGroups
        }
    }

    public suspend fun count(): Stats {
        return database.execute { connection ->
            val encryptedGroups: Long
            val validGroups: Long
            val emptyGroups: Long

            connection.prepareStatement(
                """
                SELECT
                    COUNT(*),
                    COUNT(*) FILTER (WHERE c.key_id IS NOT NULL),
                    COUNT(*) FILTER (WHERE c.key_id IS NULL AND c.empty_loc)
                FROM containers c
                WHERE c.encrypted
            """.trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rows ->
                    check(rows.next())

                    encryptedGroups = rows.getLong(1)
                    validGroups = rows.getLong(2)
                    emptyGroups = rows.getLong(3)
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
                    Stats(allKeys, validKeys, encryptedGroups, validGroups, emptyGroups)
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

    public suspend fun analyse(): String {
        val keys = exportValid()

        val process = ProcessBuilder("ent")
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        DataOutputStream(BufferedOutputStream(process.outputStream)).use { out ->
            for (key in keys) {
                out.writeInt(key.k0)
                out.writeInt(key.k1)
                out.writeInt(key.k2)
                out.writeInt(key.k3)
            }
        }

        val analysis = process.inputStream.readAllBytes().toString(Charsets.UTF_8)

        val status = process.waitFor()
        if (status != 0) {
            throw Exception("ent failed: $status")
        }

        return analysis
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
            SELECT DISTINCT (k.key).k0, (k.key).k1, (k.key).k2, (k.key).k3, k.id
            FROM keys k
            JOIN containers c ON c.key_id = k.id
            ORDER BY k.id ASC
        """.trimIndent()
    }
}
