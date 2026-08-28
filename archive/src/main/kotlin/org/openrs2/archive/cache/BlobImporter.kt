package org.openrs2.archive.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.DefaultByteBufHolder
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.buffer.crc32
import org.openrs2.crypto.sha1
import org.openrs2.crypto.whirlpool
import org.openrs2.db.Database
import java.sql.Connection

@Singleton
public class BlobImporter @Inject constructor(
    private val database: Database
) {
    public abstract class Blob(
        buf: ByteBuf
    ) : DefaultByteBufHolder(buf) {
        public val bytes: ByteArray = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false)
        public val crc32: Int = buf.crc32()
        public val sha1: ByteArray = buf.sha1()
        public val whirlpool: ByteArray = buf.whirlpool()
    }

    internal fun prepare(connection: Connection) {
        connection.prepareStatement(
            """
            LOCK TABLE containers IN EXCLUSIVE MODE
            """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        connection.prepareStatement(
            """
            CREATE TEMPORARY TABLE tmp_container_hashes (
                index INTEGER NOT NULL,
                whirlpool BYTEA NOT NULL
            ) ON COMMIT DROP
        """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        connection.prepareStatement(
            """
            CREATE TEMPORARY TABLE tmp_containers (
                index INTEGER NOT NULL,
                crc32 INTEGER NOT NULL,
                whirlpool BYTEA NOT NULL,
                uncompressed_length INTEGER NULL,
                uncompressed_crc32 INTEGER NULL,
                data BYTEA NOT NULL,
                encrypted BOOLEAN NOT NULL,
                empty_loc BOOLEAN NULL
            ) ON COMMIT DROP
            """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        connection.prepareStatement(
            """
            CREATE TEMPORARY TABLE tmp_blobs (
                index INTEGER NOT NULL,
                crc32 INTEGER NOT NULL,
                sha1 BYTEA NOT NULL,
                whirlpool BYTEA NOT NULL,
                data BYTEA NOT NULL
            ) ON COMMIT DROP
            """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }
    }

    public fun addBlob(connection: Connection, blob: Blob): Long {
        return addBlobs(connection, listOf(blob)).single()
    }

    public fun addBlobs(connection: Connection, blobs: List<Blob>): List<Long> {
        connection.prepareStatement(
            """
            TRUNCATE TABLE tmp_blobs
            """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        connection.prepareStatement(
            """
            INSERT INTO tmp_blobs (index, crc32, sha1, whirlpool, data)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { stmt ->
            for ((i, blob) in blobs.withIndex()) {
                stmt.setInt(1, i)
                stmt.setInt(2, blob.crc32)
                stmt.setBytes(3, blob.sha1)
                stmt.setBytes(4, blob.whirlpool)
                stmt.setBytes(5, blob.bytes)

                stmt.addBatch()
            }

            stmt.executeBatch()
        }

        connection.prepareStatement(
            """
            INSERT INTO blobs (crc32, sha1, whirlpool, data)
            SELECT t.crc32, t.sha1, t.whirlpool, t.data
            FROM tmp_blobs t
            LEFT JOIN blobs b ON b.whirlpool = t.whirlpool
            WHERE b.whirlpool IS NULL
            ON CONFLICT DO NOTHING
            """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        val ids = mutableListOf<Long>()

        connection.prepareStatement(
            """
            SELECT b.id
            FROM tmp_blobs t
            JOIN blobs b ON b.whirlpool = t.whirlpool
            ORDER BY t.index ASC
            """.trimIndent()
        ).use { stmt ->
            stmt.executeQuery().use { rows ->
                while (rows.next()) {
                    ids += rows.getLong(1)
                }
            }
        }

        check(ids.size == blobs.size)
        return ids
    }
}
