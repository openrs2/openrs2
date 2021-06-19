package org.openrs2.archive.game

import org.openrs2.db.Database
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
                SELECT id, url, build, last_master_index_id
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

                    var build: Int? = rows.getInt(3)
                    if (rows.wasNull()) {
                        build = null
                    }

                    var lastMasterIndexId: Int? = rows.getInt(4)
                    if (rows.wasNull()) {
                        lastMasterIndexId = null
                    }

                    return@execute Game(id, url, build, lastMasterIndexId)
                }
            }
        }
    }
}
