package org.openrs2.archive.container

import java.sql.Connection

public object ContainerImporter {
    public fun prepare(connection: Connection) {
        connection.prepareStatement(
            """
            LOCK TABLE containers IN EXCLUSIVE MODE
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
                data BYTEA NOT NULL,
                encrypted BOOLEAN NOT NULL
            ) ON COMMIT DROP
        """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }
    }

    public fun addContainer(connection: Connection, container: Container): Long {
        return addContainers(connection, listOf(container)).single()
    }

    public fun addContainers(connection: Connection, containers: List<Container>): List<Long> {
        connection.prepareStatement(
            """
            TRUNCATE TABLE tmp_containers
        """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        connection.prepareStatement(
            """
            INSERT INTO tmp_containers (index, crc32, whirlpool, data, encrypted)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        ).use { stmt ->
            for ((i, container) in containers.withIndex()) {
                stmt.setInt(1, i)
                stmt.setInt(2, container.crc32)
                stmt.setBytes(3, container.whirlpool)
                stmt.setBytes(4, container.bytes)
                stmt.setBoolean(5, container.encrypted)
                stmt.addBatch()
            }

            stmt.executeBatch()
        }

        connection.prepareStatement(
            """
            INSERT INTO containers (crc32, whirlpool, data, encrypted)
            SELECT t.crc32, t.whirlpool, t.data, t.encrypted
            FROM tmp_containers t
            LEFT JOIN containers c ON c.whirlpool = t.whirlpool
            WHERE c.whirlpool IS NULL
            ON CONFLICT DO NOTHING
        """.trimIndent()
        ).use { stmt ->
            stmt.execute()
        }

        val ids = mutableListOf<Long>()

        connection.prepareStatement(
            """
            SELECT c.id
            FROM tmp_containers t
            JOIN containers c ON c.whirlpool = t.whirlpool
            ORDER BY t.index ASC
        """.trimIndent()
        ).use { stmt ->
            stmt.executeQuery().use { rows ->
                while (rows.next()) {
                    ids += rows.getLong(1)
                }
            }
        }

        check(ids.size == containers.size)
        return ids
    }
}
