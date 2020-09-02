package dev.openrs2.cache

import dev.openrs2.compress.bzip2.Bzip2
import dev.openrs2.compress.gzip.GzipLevelOutputStream
import dev.openrs2.compress.lzma.Lzma
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream

public enum class Js5CompressionType {
    NONE,
    BZIP2,
    GZIP,
    LZMA;

    public fun createInputStream(input: InputStream, length: Int): InputStream {
        return when (this) {
            NONE -> input
            BZIP2 -> Bzip2.createHeaderlessInputStream(input)
            GZIP -> GZIPInputStream(input)
            LZMA -> Lzma.createHeaderlessInputStream(input, length.toLong())
        }
    }

    public fun createOutputStream(output: OutputStream): OutputStream {
        return when (this) {
            NONE -> output
            BZIP2 -> Bzip2.createHeaderlessOutputStream(output)
            GZIP -> GzipLevelOutputStream(output, Deflater.BEST_COMPRESSION)
            /*
             * LZMA at -9 has significantly higher CPU/memory requirements for
             * both compression _and_ decompression, so we use the default of
             * -6. Using a higher level for the typical file size in the
             * RuneScape cache probably provides insignificant returns, as
             * described in the LZMA documentation.
             */
            LZMA -> Lzma.createHeaderlessOutputStream(output, Lzma.DEFAULT_COMPRESSION)
        }
    }

    public companion object {
        private val values = values()

        public fun fromOrdinal(ordinal: Int): Js5CompressionType? {
            return if (ordinal >= 0 && ordinal < values.size) {
                values[ordinal]
            } else {
                null
            }
        }
    }
}
