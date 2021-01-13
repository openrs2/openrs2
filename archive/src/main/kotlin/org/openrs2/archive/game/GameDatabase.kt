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
                SELECT id, hostname, port, build
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
                    val hostname: String? = rows.getString(2)

                    var port: Int? = rows.getInt(3)
                    if (rows.wasNull()) {
                        port = null
                    }

                    var build: Int? = rows.getInt(4)
                    if (rows.wasNull()) {
                        build = null
                    }

                    return@execute Game(id, hostname, port, build)
                }
            }
        }
    }
}
