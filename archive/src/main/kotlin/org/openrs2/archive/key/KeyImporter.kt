package org.openrs2.archive.key

import org.openrs2.crypto.XteaKey
import org.openrs2.db.Database
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class KeyImporter @Inject constructor(
    private val database: Database,
    private val jsonKeyReader: JsonKeyReader,
    private val downloaders: Set<KeyDownloader>
) {
    public suspend fun import(path: Path) {
        val keys = mutableSetOf<XteaKey>()

        for (file in Files.walk(path)) {
            if (!Files.isRegularFile(file)) {
                continue
            }

            val name = file.fileName.toString()
            val reader = when {
                name.endsWith(".hex") -> HexKeyReader
                name.endsWith(".txt") -> TextKeyReader
                name.endsWith(".json") -> jsonKeyReader
                else -> continue
            }

            Files.newInputStream(file).use { input ->
                keys += reader.read(input)
            }
        }

        keys -= XteaKey.ZERO

        import(keys)
    }

    public suspend fun download() {
        val seenUrls = database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT url FROM keysets
            """.trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rows ->
                    val urls = mutableSetOf<String>()
                    while (rows.next()) {
                        urls += rows.getString(1)
                    }
                    return@execute urls
                }
            }
        }

        val keys = mutableSetOf<XteaKey>()
        val urls = mutableSetOf<String>()

        for (downloader in downloaders) {
            for (url in downloader.getMissingUrls(seenUrls)) {
                keys += downloader.download(url)
                urls += url
            }
        }

        database.execute { connection ->
            connection.prepareStatement(
                """
                INSERT INTO keysets (url)
                VALUES (?)
                ON CONFLICT DO NOTHING
            """.trimIndent()
            ).use { stmt ->
                for (url in urls) {
                    stmt.setString(1, url)
                    stmt.addBatch()
                }

                stmt.executeBatch()
            }

            import(connection, keys)
        }
    }

    public suspend fun import(keys: Iterable<XteaKey>) {
        database.execute { connection ->
            import(connection, keys)
        }
    }

    private fun import(connection: Connection, keys: Iterable<XteaKey>) {
        connection.prepareStatement(
            """
            LOCK TABLE keys IN EXCLUSIVE MODE
        """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        connection.prepareStatement(
            """
            CREATE TEMPORARY TABLE tmp_keys (
                key xtea_key NOT NULL
            ) ON COMMIT DROP
        """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        connection.prepareStatement(
            """
            INSERT INTO tmp_keys (key)
            VALUES (ROW(?, ?, ?, ?))
        """.trimIndent()
        ).use { stmt ->
            for (key in keys) {
                if (key.isZero) {
                    continue
                }

                stmt.setInt(1, key.k0)
                stmt.setInt(2, key.k1)
                stmt.setInt(3, key.k2)
                stmt.setInt(4, key.k3)
                stmt.addBatch()
            }

            stmt.executeBatch()
        }

        connection.prepareStatement(
            """
            INSERT INTO keys (key)
            SELECT t.key
            FROM tmp_keys t
            LEFT JOIN keys k ON k.key = t.key
            WHERE k.key IS NULL
            ON CONFLICT DO NOTHING
        """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }
    }
}
