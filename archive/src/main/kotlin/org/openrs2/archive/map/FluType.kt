package org.openrs2.archive.map

import io.netty.buffer.ByteBuf

public data class FluType(
    var color: Int = 0
) {
    public companion object {
        public fun read(buf: ByteBuf): FluType {
            val type = FluType()

            while (true) {
                val code = buf.readUnsignedByte().toInt()
                if (code == 0) {
                    break
                } else if (code == 1) {
                    type.color = buf.readUnsignedMedium()
                } else if (code == 2) {
                    buf.skipBytes(2)
                } else if (code == 3) {
                    buf.skipBytes(2)
                } else if (code == 4) {
                    // empty
                } else if (code == 5) {
                    // empty
                } else {
                    throw IllegalArgumentException("Unsupported code: $code")
                }
            }

            return type
        }
    }
}
