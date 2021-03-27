package org.openrs2.archive.key

import org.openrs2.crypto.XteaKey
import org.openrs2.db.Database
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class KeyExporter @Inject constructor(
    private val database: Database
) {
    public suspend fun exportValid(): List<XteaKey> {
        return database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT (k.key).k0, (k.key).k1, (k.key).k2, (k.key).k3
                FROM keys k
                JOIN containers c ON c.key_id = k.id
                ORDER BY k.id ASC
            """.trimIndent()
            ).use { stmt ->
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
}
