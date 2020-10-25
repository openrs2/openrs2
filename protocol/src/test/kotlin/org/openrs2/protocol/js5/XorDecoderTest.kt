package org.openrs2.protocol.js5

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.embedded.EmbeddedChannel
import org.openrs2.buffer.copiedBuffer
import org.openrs2.buffer.use
import kotlin.test.Test
import kotlin.test.assertEquals

object XorDecoderTest {
    @Test
    fun testDecode() {
        testDecode(0, "OpenRS2", false)
        testDecode(0, "OpenRS2", true)
        testDecode(32, "oPENrs\u0012", false)
        testDecode(32, "oPENrs\u0012", true)
    }

    private fun testDecode(key: Int, expected: String, direct: Boolean) {
        val decoder = XorDecoder()
        decoder.key = key

        val channel = EmbeddedChannel(decoder)
        if (direct) {
            PooledByteBufAllocator.DEFAULT.ioBuffer().use { buf ->
                buf.writeCharSequence("OpenRS2", Charsets.UTF_8)
                channel.writeInbound(buf.retain())
            }
        } else {
            channel.writeInbound(copiedBuffer("OpenRS2"))
        }

        channel.readInbound<ByteBuf>().use { actual ->
            copiedBuffer(expected).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }
}
