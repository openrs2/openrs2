package dev.openrs2.bundler

import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.Deflater

class Resource(
    val source: String,
    val destination: String,
    val digest: ByteArray,
    val uncompressedSize: Int,
    val content: ByteArray
) {
    val compressedSize: Int
        get() = content.size

    init {
        require(digest.size == 20)
    }

    companion object {
        fun compress(source: String, destination: String, uncompressed: ByteArray): Resource {
            val crc = CRC32()
            crc.update(uncompressed)

            val sourceWithCrc = source.replace(".", "_${crc.value.toInt()}.")

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

            return Resource(sourceWithCrc, destination, digest.digest(), uncompressed.size, content)
        }

        private fun compress(source: String, destination: String, resource: String): Resource {
            val path = "/dev/openrs2/natives/$resource"
            val uncompressed = Resource::class.java.getResourceAsStream(path).use { it.readBytes() }
            return compress(source, destination, uncompressed)
        }

        fun compressGlResources() = listOf(
            // Windows i386
            listOf(
                compress("jaggl_0_0.lib", "jaggl.dll", "windows-i386/jaggl.dll")
            ),

            // Windows amd64
            listOf(
                compress("jaggl_1_0.lib", "jaggl.dll", "windows-amd64/jaggl.dll")
            ),

            // macOS i386
            listOf(
                compress("jaggl_2_0.lib", "libjaggl.dylib", "mac-i386/libjaggl.dylib")
            ),

            // macOS amd64
            listOf(
                compress("jaggl_3_0.lib", "libjaggl.dylib", "mac-amd64/libjaggl.dylib")
            ),

            // Linux i386
            listOf(
                compress("jaggl_4_0.lib", "libjaggl.so", "linux-i386/libjaggl.so"),
                compress("jaggl_4_1.lib", "libjaggl_dri.so", "linux-i386/libjaggl_dri.so")
            ),

            // Linux amd64
            listOf(
                compress("jaggl_5_0.lib", "libjaggl.so", "linux-amd64/libjaggl.so"),
                compress("jaggl_5_1.lib", "libjaggl_dri.so", "linux-amd64/libjaggl_dri.so")
            )
        )

        fun compressMiscResources() = listOf(
            compress("jagmisc_0.lib", "jagmisc.dll", "windows-i386/jagmisc.dll"),
            compress("jagmisc_1.lib", "jagmisc.dll", "windows-amd64/jagmisc.dll")
        )
    }
}
