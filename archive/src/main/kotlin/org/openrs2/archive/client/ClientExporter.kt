package org.openrs2.archive.client

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.DefaultByteBufHolder
import io.netty.buffer.Unpooled
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.archive.cache.CacheExporter
import org.openrs2.db.Database
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Singleton
public class ClientExporter @Inject constructor(
    private val database: Database
) {
    public data class ArtifactSummary(
        public val id: Long,
        public val game: String,
        public val environment: String,
        public val build: CacheExporter.Build?,
        public val timestamp: Instant?,
        public val type: ArtifactType,
        public val format: ArtifactFormat,
        public val os: OperatingSystem,
        public val arch: Architecture,
        public val jvm: Jvm,
        public val size: Int
    ) {
        public val name: String
            get() {
                val builder = StringBuilder()
                builder.append(format.getPrefix(os))

                when (type) {
                    ArtifactType.CLIENT -> builder.append(game)
                    ArtifactType.CLIENT_GL -> builder.append("${game}_gl")
                    ArtifactType.GLUEGEN_RT -> builder.append("gluegen-rt")
                    else -> builder.append(type.name.lowercase())
                }

                if (jvm == Jvm.MICROSOFT) {
                    builder.append("ms")
                }

                if (os != OperatingSystem.INDEPENDENT) {
                    builder.append('-')
                    builder.append(os.name.lowercase())
                }

                if (arch != Architecture.INDEPENDENT) {
                    builder.append('-')
                    builder.append(arch.name.lowercase())
                }

                if (build != null) {
                    builder.append("-b")
                    builder.append(build)
                }

                if (timestamp != null) {
                    builder.append('-')
                    builder.append(
                        timestamp
                            .atOffset(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"))
                    )
                }

                builder.append("-openrs2#")
                builder.append(id)

                builder.append('.')
                builder.append(format.getExtension(os))

                return builder.toString()
            }
    }

    public data class ArtifactSource(
        public val name: String?,
        public val description: String?,
        public val url: String?
    )

    public data class ArtifactLinkExport(
        public val id: Long?,
        public val build: CacheExporter.Build?,
        public val timestamp: Instant?,
        public val link: ArtifactLink
    )

    public class Artifact(
        public val summary: ArtifactSummary,
        public val crc32: Int,
        public val sha1: ByteArray,
        public val sources: List<ArtifactSource>,
        public val links: List<ArtifactLinkExport>
    ) {
        public val sha1Hex: String
            get() = ByteBufUtil.hexDump(sha1)
    }

    public class ArtifactExport(
        public val summary: ArtifactSummary,
        buf: ByteBuf
    ) : DefaultByteBufHolder(buf)

    public suspend fun list(): List<ArtifactSummary> {
        return database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT
                    a.blob_id,
                    g.name,
                    e.name,
                    a.build_major,
                    a.build_minor,
                    a.timestamp,
                    a.type,
                    a.format,
                    a.os,
                    a.arch,
                    a.jvm,
                    length(b.data) AS size
                FROM artifacts a
                JOIN blobs b ON b.id = a.blob_id
                JOIN games g ON g.id = a.game_id
                JOIN environments e ON e.id = a.environment_id
                ORDER BY a.build_major ASC, a.timestamp ASC, a.type ASC, a.format ASC, a.os ASC, a.arch ASC, a.jvm ASC
            """.trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rows ->
                    val artifacts = mutableListOf<ArtifactSummary>()

                    while (rows.next()) {
                        val id = rows.getLong(1)
                        val game = rows.getString(2)
                        val environment = rows.getString(3)

                        var buildMajor: Int? = rows.getInt(4)
                        if (rows.wasNull()) {
                            buildMajor = null
                        }

                        var buildMinor: Int? = rows.getInt(5)
                        if (rows.wasNull()) {
                            buildMinor = null
                        }

                        val build = if (buildMajor != null) {
                            CacheExporter.Build(buildMajor, buildMinor)
                        } else {
                            null
                        }

                        val timestamp = rows.getTimestamp(6)?.toInstant()
                        val type = ArtifactType.valueOf(rows.getString(7).uppercase())
                        val format = ArtifactFormat.valueOf(rows.getString(8).uppercase())
                        val os = OperatingSystem.valueOf(rows.getString(9).uppercase())
                        val arch = Architecture.valueOf(rows.getString(10).uppercase())
                        val jvm = Jvm.valueOf(rows.getString(11).uppercase())
                        val size = rows.getInt(12)

                        artifacts += ArtifactSummary(
                            id,
                            game,
                            environment,
                            build,
                            timestamp,
                            type,
                            format,
                            os,
                            arch,
                            jvm,
                            size
                        )
                    }

                    return@execute artifacts
                }
            }
        }
    }

    public suspend fun get(id: Long): Artifact? {
        return database.execute { connection ->
            val sources = mutableListOf<ArtifactSource>()
            val links = mutableListOf<ArtifactLinkExport>()

            connection.prepareStatement(
                """
                SELECT DISTINCT name, description, url
                FROM artifact_sources
                WHERE blob_id = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setLong(1, id)

                stmt.executeQuery().use { rows ->
                    while (rows.next()) {
                        val name = rows.getString(1)
                        val description = rows.getString(2)
                        val url = rows.getString(3)

                        sources += ArtifactSource(name, description, url)
                    }
                }
            }

            connection.prepareStatement(
                """
                SELECT
                    a.blob_id,
                    a.build_major,
                    a.build_minor,
                    a.timestamp,
                    l.type,
                    l.format,
                    l.os,
                    l.arch,
                    l.jvm,
                    COALESCE(l.crc32, b.crc32),
                    l.sha1,
                    COALESCE(l.size, length(b.data))
                FROM artifact_links l
                LEFT JOIN blobs b ON b.sha1 = l.sha1
                LEFT JOIN artifacts a ON a.blob_id = b.id
                WHERE l.blob_id = ?
                ORDER BY l.type, l.format, l.os, l.arch, l.jvm
            """.trimIndent()
            ).use { stmt ->
                stmt.setLong(1, id)

                stmt.executeQuery().use { rows ->
                    while (rows.next()) {
                        var linkId: Long? = rows.getLong(1)
                        if (rows.wasNull()) {
                            linkId = null
                        }

                        var buildMajor: Int? = rows.getInt(2)
                        if (rows.wasNull()) {
                            buildMajor = null
                        }

                        var buildMinor: Int? = rows.getInt(3)
                        if (rows.wasNull()) {
                            buildMinor = null
                        }

                        val build = if (buildMajor != null) {
                            CacheExporter.Build(buildMajor, buildMinor)
                        } else {
                            null
                        }

                        val timestamp = rows.getTimestamp(4)?.toInstant()
                        val type = ArtifactType.valueOf(rows.getString(5).uppercase())
                        val format = ArtifactFormat.valueOf(rows.getString(6).uppercase())
                        val os = OperatingSystem.valueOf(rows.getString(7).uppercase())
                        val arch = Architecture.valueOf(rows.getString(8).uppercase())
                        val jvm = Jvm.valueOf(rows.getString(9).uppercase())

                        var crc32: Int? = rows.getInt(10)
                        if (rows.wasNull()) {
                            crc32 = null
                        }

                        val sha1 = rows.getBytes(11)

                        var size: Int? = rows.getInt(12)
                        if (rows.wasNull()) {
                            size = null
                        }

                        links += ArtifactLinkExport(
                            linkId,
                            build,
                            timestamp,
                            ArtifactLink(
                                type,
                                format,
                                os,
                                arch,
                                jvm,
                                crc32,
                                sha1,
                                size
                            )
                        )
                    }
                }
            }

            connection.prepareStatement(
                """
                SELECT
                    g.name,
                    e.name,
                    a.build_major,
                    a.build_minor,
                    a.timestamp,
                    a.type,
                    a.format,
                    a.os,
                    a.arch,
                    a.jvm,
                    length(b.data) AS size,
                    b.crc32,
                    b.sha1
                FROM artifacts a
                JOIN games g ON g.id = a.game_id
                JOIN environments e ON e.id = a.environment_id
                JOIN blobs b ON b.id = a.blob_id
                WHERE a.blob_id = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setLong(1, id)

                stmt.executeQuery().use { rows ->
                    if (!rows.next()) {
                        return@execute null
                    }

                    val game = rows.getString(1)
                    val environment = rows.getString(2)

                    var buildMajor: Int? = rows.getInt(3)
                    if (rows.wasNull()) {
                        buildMajor = null
                    }

                    var buildMinor: Int? = rows.getInt(4)
                    if (rows.wasNull()) {
                        buildMinor = null
                    }

                    val build = if (buildMajor != null) {
                        CacheExporter.Build(buildMajor, buildMinor)
                    } else {
                        null
                    }

                    val timestamp = rows.getTimestamp(5)?.toInstant()
                    val type = ArtifactType.valueOf(rows.getString(6).uppercase())
                    val format = ArtifactFormat.valueOf(rows.getString(7).uppercase())
                    val os = OperatingSystem.valueOf(rows.getString(8).uppercase())
                    val arch = Architecture.valueOf(rows.getString(9).uppercase())
                    val jvm = Jvm.valueOf(rows.getString(10).uppercase())
                    val size = rows.getInt(11)
                    val crc32 = rows.getInt(12)
                    val sha1 = rows.getBytes(13)

                    return@execute Artifact(
                        ArtifactSummary(
                            id,
                            game,
                            environment,
                            build,
                            timestamp,
                            type,
                            format,
                            os,
                            arch,
                            jvm,
                            size
                        ), crc32, sha1, sources, links
                    )
                }
            }
        }
    }

    public suspend fun export(id: Long): ArtifactExport? {
        return database.execute { connection ->
            connection.prepareStatement(
                """
                SELECT
                    g.name,
                    e.name,
                    a.build_major,
                    a.build_minor,
                    a.timestamp,
                    a.type,
                    a.format,
                    a.os,
                    a.arch,
                    a.jvm,
                    b.data
                FROM artifacts a
                JOIN games g ON g.id = a.game_id
                JOIN environments e ON e.id = a.environment_id
                JOIN blobs b ON b.id = a.blob_id
                WHERE a.blob_id = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setLong(1, id)

                stmt.executeQuery().use { rows ->
                    if (!rows.next()) {
                        return@execute null
                    }

                    val game = rows.getString(1)
                    val environment = rows.getString(2)

                    var buildMajor: Int? = rows.getInt(3)
                    if (rows.wasNull()) {
                        buildMajor = null
                    }

                    var buildMinor: Int? = rows.getInt(4)
                    if (rows.wasNull()) {
                        buildMinor = null
                    }

                    val build = if (buildMajor != null) {
                        CacheExporter.Build(buildMajor, buildMinor)
                    } else {
                        null
                    }

                    val timestamp = rows.getTimestamp(5)?.toInstant()
                    val type = ArtifactType.valueOf(rows.getString(6).uppercase())
                    val format = ArtifactFormat.valueOf(rows.getString(7).uppercase())
                    val os = OperatingSystem.valueOf(rows.getString(8).uppercase())
                    val arch = Architecture.valueOf(rows.getString(9).uppercase())
                    val jvm = Jvm.valueOf(rows.getString(10).uppercase())

                    val buf = Unpooled.wrappedBuffer(rows.getBytes(11))
                    val size = buf.readableBytes()

                    return@execute ArtifactExport(
                        ArtifactSummary(
                            id,
                            game,
                            environment,
                            build,
                            timestamp,
                            type,
                            format,
                            os,
                            arch,
                            jvm,
                            size
                        ), buf
                    )
                }
            }
        }
    }
}
