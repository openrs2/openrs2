package dev.openrs2.common.io

import java.io.FilterOutputStream
import java.io.OutputStream

class SkipOutputStream(out: OutputStream, private var skipBytes: Long) : FilterOutputStream(out) {
    override fun write(b: Int) {
        if (skipBytes == 0L) {
            super.write(b)
        } else {
            skipBytes--
        }
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (len >= skipBytes) {
            super.write(b, off + skipBytes.toInt(), len - skipBytes.toInt())
            skipBytes = 0
        } else {
            skipBytes -= len
        }
    }
}
