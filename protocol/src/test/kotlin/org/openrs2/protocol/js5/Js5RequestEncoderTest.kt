package org.openrs2.protocol.js5

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import org.openrs2.buffer.use
import kotlin.test.Test
import kotlin.test.assertEquals

class Js5RequestEncoderTest {
    @Test
    fun testEncode() {
        testEncode(Js5Request.Group(true, 2, 3), byteArrayOf(0, 2, 0, 3))
        testEncode(Js5Request.Group(false, 2, 3), byteArrayOf(1, 2, 0, 3))
        testEncode(Js5Request.Rekey(0x55), byteArrayOf(4, 0x55, 0, 0))
        testEncode(Js5Request.LoggedIn, byteArrayOf(2, 0, 0, 0))
        testEncode(Js5Request.LoggedOut, byteArrayOf(3, 0, 0, 0))
        testEncode(Js5Request.Connected, byteArrayOf(6, 0, 0, 3))
        testEncode(Js5Request.Disconnect, byteArrayOf(7, 0, 0, 0))
    }

    private fun testEncode(request: Js5Request, expected: ByteArray) {
        val channel = EmbeddedChannel(Js5RequestEncoder)
        channel.writeOutbound(request)

        channel.readOutbound<ByteBuf>().use { actual ->
            Unpooled.wrappedBuffer(expected).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }
}
