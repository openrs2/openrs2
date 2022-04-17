package org.openrs2.archive.cache.finder

import com.github.michaelbull.logging.InlineLogger
import com.google.common.io.ByteStreams
import com.google.common.io.LittleEndianDataInputStream
import org.openrs2.util.charset.Cp1252Charset
import java.io.Closeable
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.time.Instant

public class CacheFinderExtractor(
    input: InputStream
) : Closeable {
    private val pushbackInput = PushbackInputStream(input)
    private val input = LittleEndianDataInputStream(pushbackInput)

    private fun readTimestamp(): FileTime {
        val lo = input.readInt().toLong()
        val hi = input.readInt().toLong() and 0xFFFFFFFF

        val seconds = (((hi shl 32) or lo) / 10_000_000) - FILETIME_TO_UNIX_EPOCH

        return FileTime.from(Instant.ofEpochSecond(seconds, lo))
    }

    private fun readName(): String {
        val bytes = ByteArray(MAX_PATH)
        input.readFully(bytes)

        var len = bytes.size
        for ((i, b) in bytes.withIndex()) {
            if (b.toInt() == 0) {
                len = i
                break
            }
        }

        return String(bytes, 0, len, Cp1252Charset)
    }

    private fun peekUnsignedByte(): Int {
        val n = pushbackInput.read()
        pushbackInput.unread(n)
        return n
    }

    public fun extract(destination: Path) {
        val newVersion = peekUnsignedByte() == 0xFE
        if (newVersion) {
            val signature = input.readInt()
            if (signature != 0x435352FE) {
                throw IOException("Invalid signature")
            }
        }

        var readDirectoryPath = true
        var number = 0
        var directorySuffix: String? = null

        while (true) {
            if (newVersion && readDirectoryPath) {
                val len = try {
                    input.readInt()
                } catch (ex: EOFException) {
                    break
                }

                val bytes = ByteArray(len)
                input.readFully(bytes)

                val path = String(bytes, Cp1252Charset)
                logger.info { "Extracting $path" }

                readDirectoryPath = false
                directorySuffix = path.substring(path.lastIndexOf('\\') + 1)
                    .replace(INVALID_CHARS, "_")

                continue
            }

            if (peekUnsignedByte() == 0xFF) {
                input.skipBytes(1)
                readDirectoryPath = true
                number++
                continue
            }

            val attributes = try {
                input.readInt()
            } catch (ex: EOFException) {
                break
            }

            val btime = readTimestamp()
            val atime = readTimestamp()
            val mtime = readTimestamp()

            val sizeHi = input.readInt().toLong()
            val sizeLo = input.readInt().toLong() and 0xFFFFFFFF
            val size = (sizeHi shl 32) or sizeLo

            input.skipBytes(8) // reserved

            val name = readName()

            input.skipBytes(14) // alternate name
            input.skipBytes(2) // padding

            val dir = if (directorySuffix != null) {
                destination.resolve("cache${number}_$directorySuffix")
            } else {
                destination.resolve("cache$number")
            }

            Files.createDirectories(dir)

            if ((attributes and FILE_ATTRIBUTE_DIRECTORY) == 0) {
                val file = dir.resolve(name)

                Files.newOutputStream(file).use { output ->
                    ByteStreams.copy(ByteStreams.limit(input, size), output)
                }

                val view = Files.getFileAttributeView(file, BasicFileAttributeView::class.java)
                view.setTimes(mtime, atime, btime)
            }
        }
    }

    override fun close() {
        input.close()
    }

    private companion object {
        private const val FILETIME_TO_UNIX_EPOCH: Long = 11644473600
        private const val MAX_PATH = 260
        private const val FILE_ATTRIBUTE_DIRECTORY = 0x10
        private val INVALID_CHARS = Regex("[^A-Za-z0-9-]")

        private val logger = InlineLogger()
    }
}
