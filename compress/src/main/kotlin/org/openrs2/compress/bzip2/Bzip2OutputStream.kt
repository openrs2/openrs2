package org.openrs2.compress.bzip2

import jnr.ffi.Runtime
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.math.min

public class Bzip2OutputStream(
    private val library: LibBzip2,
    out: OutputStream,
    blockSize: Int
) : FilterOutputStream(out) {
    private val singleByteBuf = ByteArray(1)
    private val runtime = Runtime.getRuntime(library)
    private val stream = LibBzip2.BzStream(runtime)
    private val nextIn = runtime.memoryManager.allocateDirect(BUFFER_SIZE, false)
    private val nextOut = runtime.memoryManager.allocateDirect(BUFFER_SIZE, false)
    private val buf = ByteArray(BUFFER_SIZE)
    private var closed = false

    init {
        val result = library.BZ2_bzCompressInit(stream, blockSize, 0, 0)
        if (result != LibBzip2.BZ_OK) {
            throw IOException("bzCompressInit failed: $result")
        }

        stream.nextIn.set(nextIn)
        stream.nextOut.set(nextOut)
    }

    override fun write(b: Int) {
        singleByteBuf[0] = b.toByte()
        write(singleByteBuf, 0, singleByteBuf.size)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        var off = off
        var remaining = len

        while (remaining > 0) {
            val availIn = min(remaining, BUFFER_SIZE)
            nextIn.put(0, b, off, availIn)
            stream.nextIn.set(nextIn)
            stream.availIn.set(availIn)

            stream.nextOut.set(nextOut)
            stream.availOut.set(BUFFER_SIZE)

            val result = library.BZ2_bzCompress(stream, LibBzip2.BZ_RUN)
            if (result != LibBzip2.BZ_RUN_OK) {
                throw IOException("bzCompress failed: $result")
            }

            val read = (availIn - stream.availIn.get()).toInt()
            off += read
            remaining -= read

            val written = (BUFFER_SIZE - stream.availOut.get()).toInt()
            nextOut.get(0, buf, 0, written)
            out.write(buf, 0, written)
        }
    }

    override fun close() {
        if (closed) {
            return
        }

        closed = true

        try {
            do {
                stream.nextOut.set(nextOut)
                stream.availOut.set(BUFFER_SIZE)

                val streamEnd = when (val result = library.BZ2_bzCompress(stream, LibBzip2.BZ_FINISH)) {
                    LibBzip2.BZ_STREAM_END -> true
                    LibBzip2.BZ_FINISH_OK -> false
                    else -> throw IOException("bzCompress failed: $result")
                }

                val written = (BUFFER_SIZE - stream.availOut.get()).toInt()
                nextOut.get(0, buf, 0, written)
                out.write(buf, 0, written)
            } while (!streamEnd)
        } finally {
            val result = library.BZ2_bzCompressEnd(stream)
            if (result != LibBzip2.BZ_OK) {
                throw IOException("bzCompressEnd failed: $result")
            }
        }
    }

    private companion object {
        private const val BUFFER_SIZE = 4096
    }
}
