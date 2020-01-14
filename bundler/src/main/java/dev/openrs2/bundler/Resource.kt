package dev.openrs2.bundler

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.Library
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.Deflater

class Resource(
    val source: String,
    val destination: String,
    val crc: Int,
    val digest: ByteArray,
    val uncompressedSize: Int,
    val content: ByteArray
) {
    val sourceWithCrc: String
        get() = source.replace(".", "_$crc.")

    val compressedSize: Int
        get() = content.size

    init {
        require(digest.size == 20)
    }

    fun write(dir: Path) {
        val path = dir.resolve(source)
        logger.info { "Writing resource $path" }
        Files.write(path, content)
    }

    companion object {
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

        fun compressJar(source: String, destination: String, library: Library): Resource {
            ByteArrayOutputStream().use { out ->
                library.writeJar(out)
                return compress(source, destination, out.toByteArray())
            }
        }

        fun compressPack(source: String, destination: String, library: Library): Resource {
            ByteArrayOutputStream().use { out ->
                library.writePack(out)
                return compress(source, destination, out.toByteArray())
            }
        }

        fun compressJs5(source: String, destination: String, library: Library): Resource {
            ByteArrayOutputStream().use { out ->
                library.writeJs5(out)
                return compress(source, destination, out.toByteArray())
            }
        }

        private fun compressNative(source: String, destination: String, resource: String): Resource {
            val path = "/dev/openrs2/natives/$resource"
            val uncompressed = Resource::class.java.getResourceAsStream(path).use { it.readBytes() }
            return compress(source, destination, uncompressed)
        }

        fun compressGlNatives() = listOf(
            // Windows i386
            listOf(
                compressNative("jaggl_0_0.lib", "jaggl.dll", "windows-i386/jaggl.dll")
            ),

            // Windows amd64
            listOf(
                compressNative("jaggl_1_0.lib", "jaggl.dll", "windows-amd64/jaggl.dll")
            ),

            // macOS i386
            listOf(
                compressNative("jaggl_2_0.lib", "libjaggl.dylib", "mac-i386/libjaggl.dylib")
            ),

            // macOS amd64
            listOf(
                compressNative("jaggl_3_0.lib", "libjaggl.dylib", "mac-amd64/libjaggl.dylib")
            ),

            // Linux i386
            listOf(
                compressNative("jaggl_4_0.lib", "libjaggl.so", "linux-i386/libjaggl.so"),
                compressNative("jaggl_4_1.lib", "libjaggl_dri.so", "linux-i386/libjaggl_dri.so")
            ),

            // Linux amd64
            listOf(
                compressNative("jaggl_5_0.lib", "libjaggl.so", "linux-amd64/libjaggl.so"),
                compressNative("jaggl_5_1.lib", "libjaggl_dri.so", "linux-amd64/libjaggl_dri.so")
            )
        )

        fun compressMiscNatives() = listOf(
            compressNative("jagmisc_0.lib", "jagmisc.dll", "windows-i386/jagmisc.dll"),
            compressNative("jagmisc_1.lib", "jagmisc.dll", "windows-amd64/jagmisc.dll")
        )
    }
}
