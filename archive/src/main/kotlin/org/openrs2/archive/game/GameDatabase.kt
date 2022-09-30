package org.openrs2.archive.game

import org.openrs2.db.Database
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class GameDatabase @Inject constructor(
    private val database: Database
) {
    public suspend fun getGame(name: String, environment: String, language: String): Game? {
        return database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT v.id, v.url, v.build_major, v.build_minor, v.last_master_index_id, v.language_id, g.scope_id
                FROM game_variants v
                JOIN games g ON g.id = v.game_id
                JOIN environments e ON e.id = v.environment_id
                JOIN languages l ON l.id = v.language_id
                WHERE g.name = ? AND e.name = ? AND l.iso_code = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, name)
                stmt.setString(2, environment)
                stmt.setString(3, language)

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

                    val languageId = rows.getInt(6)
                    val scopeId = rows.getInt(7)

                    return@execute Game(id, url, buildMajor, buildMinor, lastMasterIndexId, languageId, scopeId)
                }
            }
        }
    }
}
