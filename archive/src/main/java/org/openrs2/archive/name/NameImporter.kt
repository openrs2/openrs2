package org.openrs2.archive.name

import org.openrs2.db.Database
import org.openrs2.util.krHashCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class NameImporter @Inject constructor(
    private val database: Database
) {
    public suspend fun import(names: Iterable<String>) {
        database.execute { connection ->
            connection.prepareStatement(
                """
                INSERT INTO names (hash, name)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
            """.trimIndent()
            ).use { stmt ->
                for (name in names) {
                    stmt.setInt(1, name.krHashCode())
                    stmt.setString(2, name)
                    stmt.addBatch()
                }

                stmt.executeBatch()
            }
        }
    }
}
