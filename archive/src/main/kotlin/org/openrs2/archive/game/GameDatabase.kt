package org.openrs2.archive.game

import org.openrs2.crypto.Rsa
import org.openrs2.db.Database
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class GameDatabase @Inject constructor(
    private val database: Database
) {
    public suspend fun getGame(name: String): Game? {
        return database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT id, url, build_major, build_minor, last_master_index_id, key
                FROM games
                WHERE name = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, name)

                stmt.executeQuery().use { rows ->
                    if (!rows.next()) {
                        return@execute null
                    }

                    val id = rows.getInt(1)
                    val url: String? = rows.getString(2)

                    var buildMajor: Int? = rows.getInt(3)
                    if (rows.wasNull()) {
                        buildMajor = null
                    }

                    var buildMinor: Int? = rows.getInt(4)
                    if (rows.wasNull()) {
                        buildMinor = null
                    }

                    var lastMasterIndexId: Int? = rows.getInt(5)
                    if (rows.wasNull()) {
                        lastMasterIndexId = null
                    }

                    val pem = rows.getString(6)
                    val key = if (rows.wasNull()) {
                        null
                    } else {
                        StringReader(pem).use { reader ->
                            Rsa.readPublicKey(reader)
                        }
                    }

                    return@execute Game(id, url, buildMajor, buildMinor, lastMasterIndexId, key)
                }
            }
        }
    }
}
