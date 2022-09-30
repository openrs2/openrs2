package org.openrs2.compress.gzip

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.Inflater

public class GzipLaxInputStream(
    private val input: InputStream
) : InputStream() {
    private val inflater = Inflater(true)
    private val buffer = ByteArray(4096)
    private var checkedTrailer = false

    init {
        val dataInput = DataInputStream(input)
        if (dataInput.readUnsignedShort() != HEADER_MAGIC) {
            throw IOException("Invalid GZIP header magic")
        } else if (dataInput.readUnsignedByte() != METHOD_DEFLATE) {
            throw IOException("Unsupported compression method")
        }

        val flags = dataInput.readUnsignedByte()
        dataInput.skip(6)

        if ((flags and FLAG_EXTRA) != 0) {
            dataInput.skip(dataInput.readUnsignedShort().toLong())
        }

        if ((flags and FLAG_NAME) != 0) {
            while (dataInput.readUnsignedByte() != 0) {
                // empty
            }
        }

        if ((flags and FLAG_COMMENT) != 0) {
            while (dataInput.readUnsignedByte() != 0) {
                // empty
            }
        }

        if ((flags and FLAG_HEADER_CRC) != 0) {
            dataInput.skip(2)
        }
    }

    override fun read(): Int {
        val n = read(buffer, 0, 1)
        return if (n < 0) {
            -1
        } else {
            buffer[0].toInt() and 0xFF
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        while (true) {
            val n = inflater.inflate(b, off, len)
            if (n != 0) {
                return n
            }

            when {
                inflater.finished() -> {
                    checkTrailer()
                    return -1
                }

                inflater.needsInput() -> fill()
                inflater.needsDictionary() -> throw IOException("Dictionaries not supported")
            }
        }
    }

    override fun close() {
        inflater.end()
        input.close()
    }

    private fun fill() {
        val n = input.read(buffer, 0, buffer.size)
        if (n < 0) {
            throw IOException("Compressed data truncated")
        }
        inflater.setInput(buffer, 0, n)
    }

    private fun checkTrailer() {
        if (checkedTrailer) {
            return
        }

        checkedTrailer = true

        var len = TRAILER_LEN
        len -= inflater.remaining

        if (len < 0) {
            throw IOException("Compressed data overflow")
        } else if (input.skip(len) != len) {
            throw IOException("GZIP trailer missing")
        }
    }

    private companion object {
        private const val HEADER_MAGIC = 0x1F8B

        private const val METHOD_DEFLATE = 8

        private const val FLAG_HEADER_CRC = 0x2
        private const val FLAG_EXTRA = 0x4
        private const val FLAG_NAME = 0x8
        private const val FLAG_COMMENT = 0x10

        private const val TRAILER_LEN = 8L
    }
}
