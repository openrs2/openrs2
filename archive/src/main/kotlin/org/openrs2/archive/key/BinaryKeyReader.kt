package org.openrs2.archive.key

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.openrs2.buffer.use
import org.openrs2.crypto.XteaKey
import java.io.InputStream

public object BinaryKeyReader : KeyReader {
    override fun read(input: InputStream): Sequence<XteaKey> {
        Unpooled.wrappedBuffer(input.readBytes()).use { buf ->
            val len = buf.readableBytes()

            if (len == (128 * 128 * 16)) {
                val keys = read(buf, 0)
                require(XteaKey.ZERO in keys)
                return keys.asSequence()
            }

            val maybeShort = (len % 18) == 0
            val maybeInt = (len % 20) == 0

            if (maybeShort && !maybeInt) {
                val keys = read(buf, 2)
                require(XteaKey.ZERO in keys)
                return keys.asSequence()
            } else if (!maybeShort && maybeInt) {
                val keys = read(buf, 4).asSequence()
                require(XteaKey.ZERO in keys)
                return keys.asSequence()
            } else if (maybeShort && maybeInt) {
                val shortKeys = read(buf, 2)
                val intKeys = read(buf, 4)

                return if (XteaKey.ZERO in shortKeys && XteaKey.ZERO !in intKeys) {
                    shortKeys.asSequence()
                } else if (XteaKey.ZERO !in shortKeys && XteaKey.ZERO in intKeys) {
                    intKeys.asSequence()
                } else {
                    throw IllegalArgumentException("Failed to determine if map square IDs are 2 or 4 bytes")
                }
            } else {
                throw IllegalArgumentException(
                    "Binary XTEA files must be exactly 256 KiB or a multiple of 18 or 20 bytes long"
                )
            }
        }
    }

    private fun read(buf: ByteBuf, mapSquareLen: Int): Set<XteaKey> {
        val keys = mutableSetOf<XteaKey>()

        while (buf.isReadable) {
            buf.skipBytes(mapSquareLen)

            val k0 = buf.readInt()
            val k1 = buf.readInt()
            val k2 = buf.readInt()
            val k3 = buf.readInt()
            keys += XteaKey(k0, k1, k2, k3)
        }

        return keys
    }
}
