package org.openrs2.archive.map

import io.netty.buffer.ByteBuf

public data class FloType(
    var color: Int = 0,
    var texture: Int = -1,
    var blendColor: Int = -1
) {
    public companion object {
        public fun read(buf: ByteBuf): FloType {
            val type = FloType()

            while (true) {
                val code = buf.readUnsignedByte().toInt()
                if (code == 0) {
                    break
                } else if (code == 1) {
                    type.color = buf.readUnsignedMedium()
                } else if (code == 2) {
                    type.texture = buf.readUnsignedByte().toInt()
                } else if (code == 3) {
                    type.texture = buf.readUnsignedShort()
                    if (type.texture == 65535) {
                        type.texture = -1
                    }
                } else if (code == 5) {
                    // empty
                } else if (code == 7) {
                    type.blendColor = buf.readUnsignedMedium()
                } else if (code == 8) {
                    // empty
                } else if (code == 9) {
                    buf.skipBytes(2)
                } else if (code == 10) {
                    // empty
                } else if (code == 11) {
                    buf.skipBytes(1)
                } else if (code == 12) {
                    // empty
                } else if (code == 13) {
                    buf.skipBytes(3)
                } else if (code == 14) {
                    buf.skipBytes(1)
                } else if (code == 15) {
                    buf.skipBytes(2)
                } else if (code == 16) {
                    buf.skipBytes(1)
                } else {
                    throw IllegalArgumentException("Unsupported code: $code")
                }
            }

            return type
        }
    }
}
