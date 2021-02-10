package org.openrs2.protocol.js5

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.DecoderException
import org.openrs2.buffer.wrappedBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Js5RequestDecoderTest {
    @Test
    fun testDecode() {
        testDecode(byteArrayOf(0, 2, 0, 3), Js5Request.Group(true, 2, 3))
        testDecode(byteArrayOf(1, 2, 0, 3), Js5Request.Group(false, 2, 3))
        testDecode(byteArrayOf(4, 0x55, 0, 0), Js5Request.Rekey(0x55))
        testDecode(byteArrayOf(2, 0, 0, 0), Js5Request.LoggedIn)
        testDecode(byteArrayOf(3, 0, 0, 0), Js5Request.LoggedOut)
        testDecode(byteArrayOf(6, 0, 0, 3), Js5Request.Connected)
        testDecode(byteArrayOf(7, 0, 0, 0), Js5Request.Disconnect)
    }

    @Test
    fun testFragmented() {
        val channel = EmbeddedChannel(Js5RequestDecoder())
        channel.writeInbound(wrappedBuffer(0, 2))
        channel.writeInbound(wrappedBuffer(0, 3))
        assertEquals(Js5Request.Group(true, 2, 3), channel.readInbound())
    }

    @Test
    fun testUnknownOpcode() {
        val channel = EmbeddedChannel(Js5RequestDecoder())

        assertFailsWith<DecoderException> {
            channel.writeInbound(wrappedBuffer(8, 0, 0, 0))
        }
    }

    private fun testDecode(bytes: ByteArray, expected: Js5Request) {
        val channel = EmbeddedChannel(Js5RequestDecoder())

        channel.writeInbound(Unpooled.wrappedBuffer(bytes))
        assertEquals(expected, channel.readInbound())
    }
}
