package org.openrs2.protocol.js5

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.embedded.EmbeddedChannel
import org.openrs2.buffer.copiedBuffer
import org.openrs2.buffer.use
import kotlin.test.Test
import kotlin.test.assertEquals

object XorEncoderTest {
    @Test
    fun testEncode() {
        testEncode(0, "OpenRS2", false)
        testEncode(0, "OpenRS2", true)
        testEncode(32, "oPENrs\u0012", false)
        testEncode(32, "oPENrs\u0012", true)
    }

    private fun testEncode(key: Int, expected: String, direct: Boolean) {
        val encoder = XorEncoder()
        encoder.key = key

        val channel = EmbeddedChannel(encoder)
        if (direct) {
            PooledByteBufAllocator.DEFAULT.ioBuffer().use { buf ->
                buf.writeCharSequence("OpenRS2", Charsets.UTF_8)
                channel.writeOutbound(buf.retain())
            }
        } else {
            channel.writeOutbound(copiedBuffer("OpenRS2"))
        }

        channel.readOutbound<ByteBuf>().use { actual ->
            copiedBuffer(expected).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }
}
