package org.openrs2.archive.name

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.db.Database
import org.openrs2.util.krHashCode

@Singleton
public class NameImporter @Inject constructor(
    private val database: Database,
    private val downloaders: Set<NameDownloader>
) {
    public suspend fun download() {
        val names = mutableSetOf<String>()
        for (downloader in downloaders) {
            names += downloader.download()
        }
        import(names)
    }

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
