package org.openrs2.compress.bzip2

import com.github.michaelbull.logging.InlineLogger
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.openrs2.util.io.SkipOutputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.SequenceInputStream

public object Bzip2 {
    private const val BLOCK_SIZE = 1
    private val HEADER = byteArrayOf(
        'B'.code.toByte(),
        'Z'.code.toByte(),
        'h'.code.toByte(),
        ('0' + BLOCK_SIZE).code.toByte()
    )

    private val logger = InlineLogger()
    private val library: LibBzip2? = try {
        LibBzip2.load()
    } catch (ex: Throwable) {
        logger.warn(ex) {
            "Falling back to pure Java bzip2 implementation, " +
                "output may not be bit-for-bit identical to Jagex's implementation"
        }

        null
    }

    public fun createHeaderlessInputStream(input: InputStream): InputStream {
        return BZip2CompressorInputStream(SequenceInputStream(ByteArrayInputStream(HEADER), input))
    }

    public fun createHeaderlessOutputStream(output: OutputStream): OutputStream {
        val skipOutput = SkipOutputStream(output, HEADER.size.toLong())
        return if (library != null) {
            Bzip2OutputStream(library, skipOutput, BLOCK_SIZE)
        } else {
            BZip2CompressorOutputStream(skipOutput, BLOCK_SIZE)
        }
    }
}
