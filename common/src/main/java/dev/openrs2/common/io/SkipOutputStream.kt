package dev.openrs2.common.io

import java.io.OutputStream

class SkipOutputStream(private val out: OutputStream, private var skipBytes: Long) : OutputStream() {
    override fun write(b: Int) {
        if (skipBytes == 0L) {
            out.write(b)
        } else {
            skipBytes--
        }
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (len > skipBytes) {
            out.write(b, off + skipBytes.toInt(), len - skipBytes.toInt())
            skipBytes = 0
        } else {
            skipBytes -= len
        }
    }

    override fun flush() {
        out.flush()
    }

    override fun close() {
        out.close()
    }
}
