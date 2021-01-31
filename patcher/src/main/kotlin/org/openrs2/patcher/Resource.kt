package org.openrs2.patcher

import com.github.michaelbull.logging.InlineLogger
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.io.LibraryWriter
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.Deflater

public class Resource(
    public val source: String,
    public val destination: String,
    public val checksum: Int,
    public val digest: ByteArray,
    public val uncompressedSize: Int,
    public val content: ByteArray
) {
    public val sourceWithChecksum: String
        get() = source.replace(".", "_$checksum.")

    public val compressedSize: Int
        get() = content.size

    init {
        require(digest.size == 20)
    }

    public fun write(dir: Path) {
        val path = dir.resolve(source)
        logger.info { "Writing resource $path" }
        Files.write(path, content)
    }

    public companion object {
        private val logger = InlineLogger()

        private fun compress(source: String, destination: String, uncompressed: ByteArray): Resource {
            val crc = CRC32()
            crc.update(uncompressed)

            val digest = MessageDigest.getInstance("SHA-1")
            digest.update(uncompressed)

            val compressed = ByteArray(uncompressed.size)
            val compressedSize: Int
            val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
            try {
                deflater.setInput(uncompressed)
                deflater.finish()

                compressedSize = deflater.deflate(compressed)
            } finally {
                deflater.end()
            }

            val content = if (compressedSize < uncompressed.size) {
                compressed.copyOf(compressedSize)
            } else {
                uncompressed
            }

            return Resource(source, destination, crc.value.toInt(), digest.digest(), uncompressed.size, content)
        }

        public fun compressLibrary(
            source: String,
            destination: String,
            classPath: ClassPath,
            library: Library,
            writer: LibraryWriter
        ): Resource {
            ByteArrayOutputStream().use { output ->
                writer.write(output, classPath, library)
                return compress(source, destination, output.toByteArray())
            }
        }

        private fun compressNative(source: String, destination: String, resource: String): Resource {
            val path = "/org/openrs2/natives/$resource"
            val uncompressed = Resource::class.java.getResourceAsStream(path).use { it.readBytes() }
            return compress(source, destination, uncompressed)
        }

        public fun compressGlNatives(): List<List<Resource>> {
            val platforms = mutableListOf<List<Resource>>()
            var i = 0

            for (os in OperatingSystem.values()) {
                for (arch in os.architectures) {
                    val resources = mutableListOf<Resource>()

                    for ((j, library) in os.glLibraries.withIndex()) {
                        resources += compressNative(
                            source = "jaggl_${i}_$j.lib",
                            destination = library,
                            resource = "${os.id}-${arch.id}/$library"
                        )
                    }

                    platforms += resources
                    i++
                }
            }

            return platforms
        }

        public fun compressMiscNatives(): List<Resource> {
            val os = OperatingSystem.WINDOWS
            val resources = mutableListOf<Resource>()

            for ((i, arch) in os.architectures.withIndex()) {
                resources += compressNative(
                    source = "jagmisc_$i.lib",
                    destination = "jagmisc.dll",
                    resource = "${os.id}-${arch.id}/jagmisc.dll"
                )
            }

            return resources
        }
    }
}
