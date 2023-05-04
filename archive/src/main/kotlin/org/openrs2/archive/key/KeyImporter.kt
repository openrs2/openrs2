package org.openrs2.archive.key

import com.github.michaelbull.logging.InlineLogger
import org.openrs2.crypto.XteaKey
import org.openrs2.db.Database
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.Types
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class KeyImporter @Inject constructor(
    private val database: Database,
    private val jsonKeyReader: JsonKeyReader,
    private val downloaders: Set<KeyDownloader>
) {
    private data class Key(val key: XteaKey, val source: KeySource)

    public suspend fun import(path: Path) {
        val keys = mutableSetOf<XteaKey>()

        for (file in Files.walk(path)) {
            if (!Files.isRegularFile(file)) {
                continue
            }

            val name = file.fileName.toString()
            val reader = when {
                name.endsWith(".bin") -> BinaryKeyReader
                name.endsWith(".dat") -> BinaryKeyReader
                name.endsWith(".hex") -> HexKeyReader
                name.endsWith(".json") -> jsonKeyReader
                name.endsWith(".mcx") -> BinaryKeyReader
                name.endsWith(".txt") -> TextKeyReader
                else -> continue
            }

            Files.newInputStream(file).use { input ->
                keys += reader.read(input)
            }
        }

        keys -= XteaKey.ZERO

        logger.info { "Importing ${keys.size} keys" }

        import(keys, KeySource.DISK)
    }

    public suspend fun download() {
        val now = Instant.now()

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

        val keys = mutableSetOf<Key>()
        val urls = mutableSetOf<String>()

        for (downloader in downloaders) {
            try {
                for (url in downloader.getMissingUrls(seenUrls)) {
                    keys += downloader.download(url).map { key ->
                        Key(key, downloader.source)
                    }
                    urls += url
                }
            } catch (ex: IOException) {
                logger.warn(ex) { "Failed to download keys from ${downloader.source.name}" }
                continue
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

            import(connection, keys, now)
        }
    }

    public suspend fun import(keys: Iterable<XteaKey>, source: KeySource) {
        val now = Instant.now()

        database.execute { connection ->
            import(connection, keys.map { key ->
                Key(key, source)
            }, now)
        }
    }

    private fun import(connection: Connection, keys: Iterable<Key>, now: Instant) {
        val timestamp = now.atOffset(ZoneOffset.UTC)

        connection.prepareStatement(
            """
            INSERT INTO key_queue AS K (key, source, first_seen, last_seen)
            VALUES (ROW(?, ?, ?, ?), ?::key_source, ?, ?)
            ON CONFLICT (key, source) DO UPDATE SET
                first_seen = LEAST(k.first_seen, EXCLUDED.first_seen),
                last_seen = GREATEST(k.last_seen, EXCLUDED.last_seen)
            """.trimIndent()
        ).use { stmt ->
            for (key in keys) {
                if (key.key.isZero) {
                    continue
                }

                stmt.setInt(1, key.key.k0)
                stmt.setInt(2, key.key.k1)
                stmt.setInt(3, key.key.k2)
                stmt.setInt(4, key.key.k3)
                stmt.setString(5, key.source.name.lowercase())
                stmt.setObject(6, timestamp, Types.TIMESTAMP_WITH_TIMEZONE)
                stmt.setObject(7, timestamp, Types.TIMESTAMP_WITH_TIMEZONE)
                stmt.addBatch()
            }

            stmt.executeBatch()
        }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
