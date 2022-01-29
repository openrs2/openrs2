package org.openrs2.compress.bzip2

import java.io.Closeable
import java.io.Flushable
import java.io.OutputStream

public class BitOutputStream(
    private val output: OutputStream
) : Flushable, Closeable {
    private var buf: Int = 0
    private var live: Int = 0

    public fun writeBits(n: Int, v: Int) {
        while (live >= 8) {
            output.write(buf ushr 24)
            buf = buf shl 8
            live -= 8
        }

        buf = buf or (v shl (32 - live - n))
        live += n
    }

    public fun writeBoolean(v: Boolean) {
        writeBits(1, if (v) 1 else 0)
    }

    public fun writeByte(v: Int) {
        writeBits(8, v)
    }

    public fun writeInt(v: Int) {
        writeBits(32, v)
    }

    override fun flush() {
        while (live > 0) {
            output.write(buf ushr 24)
            buf = buf shl 8
            live -= 8
        }
    }

    override fun close() {
        flush()
        output.close()
    }
}
