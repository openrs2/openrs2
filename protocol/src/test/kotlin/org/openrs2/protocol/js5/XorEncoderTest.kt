package org.openrs2.protocol.js5

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
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
                buf.writeBytes("OpenRS2".toByteArray())
                channel.writeOutbound(buf.retain())
            }
        } else {
            channel.writeOutbound(Unpooled.wrappedBuffer("OpenRS2".toByteArray()))
        }

        channel.readOutbound<ByteBuf>().use { actual ->
            Unpooled.wrappedBuffer(expected.toByteArray()).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }
}
