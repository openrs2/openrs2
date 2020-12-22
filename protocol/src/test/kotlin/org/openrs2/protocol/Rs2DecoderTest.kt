package org.openrs2.protocol

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.DecoderException
import org.junit.jupiter.api.assertThrows
import org.openrs2.buffer.wrappedBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

object Rs2DecoderTest {
    @Test
    fun testDecode() {
        testDecode(byteArrayOf(0, 0x11, 0x22, 0x33, 0x44), FixedPacket(0x11223344))
        testDecode(byteArrayOf(1, 3, 0x11, 0x22, 0x33), VariableBytePacket(byteArrayOf(0x11, 0x22, 0x33)))
        testDecode(byteArrayOf(2, 0, 3, 0x11, 0x22, 0x33), VariableShortPacket(byteArrayOf(0x11, 0x22, 0x33)))
        testDecode(byteArrayOf(5), EmptyPacket)
    }

    @Test
    fun testFragmented() {
        testFragmented(byteArrayOf(0, 0x11, 0x22, 0x33, 0x44), FixedPacket(0x11223344))
        testFragmented(byteArrayOf(1, 3, 0x11, 0x22, 0x33), VariableBytePacket(byteArrayOf(0x11, 0x22, 0x33)))
        testFragmented(byteArrayOf(2, 0, 3, 0x11, 0x22, 0x33), VariableShortPacket(byteArrayOf(0x11, 0x22, 0x33)))
    }

    @Test
    fun testUnsupported() {
        val channel = EmbeddedChannel(Rs2Decoder(Protocol()))

        assertThrows<DecoderException> {
            channel.writeInbound(wrappedBuffer(0))
        }
    }

    @Test
    fun testEncryptedOpcode() {
        val decoder = Rs2Decoder(Protocol(FixedPacketCodec))
        decoder.cipher = TestStreamCipher

        val channel = EmbeddedChannel(decoder)
        channel.writeInbound(wrappedBuffer(10, 0x11, 0x22, 0x33, 0x44))

        val actual = channel.readInbound<Packet>()
        assertEquals(FixedPacket(0x11223344), actual)
    }

    @Test
    fun testSwitchProtocol() {
        val decoder = Rs2Decoder(Protocol(FixedPacketCodec))
        val channel = EmbeddedChannel(decoder)

        channel.writeInbound(wrappedBuffer(0, 0x11, 0x22, 0x33, 0x44))
        channel.readInbound<Packet>()

        assertThrows<DecoderException> {
            channel.writeInbound(wrappedBuffer(5))
        }

        decoder.protocol = Protocol(TestEmptyPacketCodec)

        channel.writeInbound(wrappedBuffer(5))

        val actual = channel.readInbound<Packet>()
        assertEquals(EmptyPacket, actual)

        assertThrows<DecoderException> {
            channel.writeInbound(wrappedBuffer(0, 0x11, 0x22, 0x33, 0x44))
        }
    }

    private fun testDecode(buf: ByteArray, expected: Packet) {
        val channel = EmbeddedChannel(Rs2Decoder(Protocol(
            FixedPacketCodec,
            VariableBytePacketCodec,
            VariableShortPacketCodec,
            VariableByteOptimisedPacketCodec,
            VariableShortOptimisedPacketCodec,
            TestEmptyPacketCodec
        )))
        channel.writeInbound(Unpooled.wrappedBuffer(buf))

        val actual = channel.readInbound<Packet>()
        assertEquals(expected, actual)
    }

    private fun testFragmented(buf: ByteArray, expected: Packet) {
        val channel = EmbeddedChannel(Rs2Decoder(Protocol(
            FixedPacketCodec,
            VariableBytePacketCodec,
            VariableShortPacketCodec,
            VariableByteOptimisedPacketCodec,
            VariableShortOptimisedPacketCodec
        )))

        for (b in buf) {
            channel.writeInbound(wrappedBuffer(b))
        }

        val actual = channel.readInbound<Packet>()
        assertEquals(expected, actual)
    }
}
