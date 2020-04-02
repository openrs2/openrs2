package dev.openrs2.compress.bzip2

import dev.openrs2.util.io.SkipOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.SequenceInputStream

object Bzip2 {
    private const val BLOCK_SIZE = 1
    private val HEADER = byteArrayOf('B'.toByte(), 'Z'.toByte(), 'h'.toByte(), ('0' + BLOCK_SIZE).toByte())

    fun createHeaderlessInputStream(input: InputStream): InputStream {
        return BZip2CompressorInputStream(SequenceInputStream(ByteArrayInputStream(HEADER), input))
    }

    fun createHeaderlessOutputStream(output: OutputStream): OutputStream {
        return BZip2CompressorOutputStream(SkipOutputStream(output, HEADER.size.toLong()), BLOCK_SIZE)
    }
}
