package org.openrs2.cache

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.name

@Singleton
public class RuneLiteStore @Inject constructor(
    private val alloc: ByteBufAllocator
) {
    public fun unpack(input: Path, output: Store) {
        output.create(Store.ARCHIVESET)

        for (path in Files.list(input)) {
            val name = path.name
            if (!name.endsWith(".flatcache")) {
                continue
            }

            val archive = name.removeSuffix(".flatcache").toIntOrNull() ?: continue
            unpackArchive(path, archive, output)
        }
    }

    private fun unpackArchive(path: Path, archive: Int, output: Store) {
        val index = Js5Index(Js5Protocol.ORIGINAL)
        var indexChecksum = 0

        Files.newBufferedReader(path).useLines { lines ->
            var group: Js5Index.MutableGroup? = null

            for (line in lines) {
                val pair = line.split('=', limit = 2)
                if (pair.size != 2) {
                    throw StoreCorruptException("Missing = in line")
                }

                val (key, value) = pair

                if (group == null) {
                    when (key) {
                        "protocol" -> {
                            val protocolId = value.toIntOrNull()
                                ?: throw StoreCorruptException("Protocol must be an integer")

                            index.protocol = Js5Protocol.fromId(protocolId)
                                ?: throw StoreCorruptException("Protocol number not supported")
                        }

                        "revision" -> {
                            index.version = value.toIntOrNull()
                                ?: throw StoreCorruptException("Revision must be an integer")
                        }

                        "compression" -> Unit

                        "crc" -> {
                            indexChecksum = value.toIntOrNull()
                                ?: throw StoreCorruptException("Index CRC must be an integer")
                        }

                        "named" -> Unit

                        "id" -> {
                            val id = value.toIntOrNull()
                                ?: throw StoreCorruptException("Group ID must be an integer")

                            group = index.createOrGet(id)
                        }

                        else -> throw StoreCorruptException("Unknown key in archive context: $key")
                    }
                } else {
                    when (key) {
                        "namehash" -> {
                            group.nameHash = value.toIntOrNull()
                                ?: throw StoreCorruptException("Group name hash must be an integer")

                            if (group.nameHash != 0) {
                                index.hasNames = true
                            }
                        }

                        "revision" -> {
                            group.version = value.toIntOrNull()
                                ?: throw StoreCorruptException("Revision must be an integer")
                        }

                        "crc" -> {
                            group.checksum = value.toIntOrNull()
                                ?: throw StoreCorruptException("Group CRC must be an integer")
                        }

                        "contents" -> {
                            Unpooled.wrappedBuffer(Base64.getDecoder().decode(value)).use { buf ->
                                // Some groups have version trailers and some don't. Fix that up here.
                                if (group!!.checksum != buf.crc32()) {
                                    if (group!!.version != VersionTrailer.strip(buf)) {
                                        throw StoreCorruptException("Group version does not match contents")
                                    }

                                    if (group!!.checksum != buf.crc32()) {
                                        throw StoreCorruptException("Group CRC does not match contents")
                                    }
                                }

                                alloc.buffer(2, 2).use { versionBuf ->
                                    versionBuf.writeShort(group!!.version)

                                    Unpooled.wrappedBuffer(buf.retain(), versionBuf.retain()).use { combinedBuf ->
                                        output.write(archive, group!!.id, combinedBuf)
                                    }
                                }
                            }
                        }

                        "compression" -> Unit

                        "file" -> {
                            val pair = value.split('=', limit = 2)
                            if (pair.size != 2) {
                                throw StoreCorruptException("Missing = in file line")
                            }

                            val id = pair[0].toIntOrNull()
                                ?: throw StoreCorruptException("File ID must be an integer")

                            val file = group.createOrGet(id)

                            file.nameHash = pair[1].toIntOrNull()
                                ?: throw StoreCorruptException("File name hash must be an integer")

                            if (file.nameHash != 0) {
                                index.hasNames = true
                            }
                        }

                        "id" -> {
                            val id = value.toIntOrNull()
                                ?: throw StoreCorruptException("Group ID must be an integer")

                            group = index.createOrGet(id)
                        }

                        else -> throw StoreCorruptException("Unknown key in group context: $key")
                    }
                }
            }
        }

        alloc.buffer().use { uncompressed ->
            index.write(uncompressed)

            val matching = Js5CompressionType.values().count { type ->
                Js5Compression.compress(uncompressed.slice(), type).use { compressed ->
                    val checksum = compressed.crc32()

                    if (checksum == indexChecksum) {
                        output.write(Store.ARCHIVESET, archive, compressed)
                        return@use true
                    }

                    return@use false
                }
            }

            if (matching != 1) {
                throw StoreCorruptException("Failed to reconstruct Js5Index")
            }
        }
    }
}
