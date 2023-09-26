package org.openrs2.archive.client

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import org.openrs2.archive.cache.CacheExporter
import org.openrs2.archive.cache.CacheImporter
import java.time.Instant

public class Artifact(
    data: ByteBuf,
    public val game: String,
    public val environment: String,
    public val build: CacheExporter.Build?,
    public val timestamp: Instant?,
    public val type: ArtifactType,
    public val format: ArtifactFormat,
    public val os: OperatingSystem,
    public val arch: Architecture,
    public val jvm: Jvm,
    public val links: List<ArtifactLink>
) : CacheImporter.Blob(data)

public data class ArtifactLink(
    val type: ArtifactType,
    val format: ArtifactFormat,
    val os: OperatingSystem,
    val arch: Architecture,
    val jvm: Jvm,
    val crc32: Int?,
    val sha1: ByteArray,
    val size: Int?
) {
    public val sha1Hex: String
        get() = ByteBufUtil.hexDump(sha1)
}
