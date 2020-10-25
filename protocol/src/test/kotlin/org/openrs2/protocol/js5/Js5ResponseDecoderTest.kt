package org.openrs2.protocol.js5

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.DecoderException
import org.junit.jupiter.api.assertThrows
import org.openrs2.buffer.use
import org.openrs2.buffer.wrappedBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

object Js5ResponseDecoderTest {
    @Test
    fun testDecode() {
        testDecode("508.dat", "508-prefetch.dat", true)
        testDecode("508.dat", "508-urgent.dat", false)

        testDecode("509.dat", "509-prefetch.dat", true)
        testDecode("509.dat", "509-urgent.dat", false)

        testDecode("1019.dat", "1019-prefetch.dat", true)
        testDecode("1019.dat", "1019-urgent.dat", false)

        testDecode("1020.dat", "1020-prefetch.dat", true)
        testDecode("1020.dat", "1020-urgent.dat", false)

        testDecode("1530.dat", "1530-prefetch.dat", true)
        testDecode("1530.dat", "1530-urgent.dat", false)

        testDecode("1531.dat", "1531-prefetch.dat", true)
        testDecode("1531.dat", "1531-urgent.dat", false)
    }

    @Test
    fun testDecodeFragmented() {
        val channel = EmbeddedChannel(Js5ResponseDecoder())

        channel.writeInbound(wrappedBuffer(2, 0, 3, 0, 0, 0, 0))
        channel.writeInbound(wrappedBuffer(7, 'O'.toByte(), 'p'.toByte(), 'e'.toByte(), 'n'.toByte()))
        channel.writeInbound(wrappedBuffer('R'.toByte(), 'S'.toByte(), '2'.toByte()))

        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            buf.writeByte(0)
            buf.writeInt(7)
            buf.writeCharSequence("OpenRS2", Charsets.UTF_8)

            val expected = Js5Response(false, 2, 3, buf)

            channel.readInbound<Js5Response>().use { actual ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testDecodeNegativeLength() {
        val channel = EmbeddedChannel(Js5ResponseDecoder())

        assertThrows<DecoderException> {
            channel.writeInbound(wrappedBuffer(2, 0, 3, 0, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))
        }
    }

    @Test
    fun testDecodeOverflowUncompressed() {
        val channel = EmbeddedChannel(Js5ResponseDecoder())

        assertThrows<DecoderException> {
            channel.writeInbound(wrappedBuffer(2, 0, 3, 0, 0x7F, 0xFF.toByte(), 0xFF.toByte(), 0xFB.toByte()))
        }
    }

    @Test
    fun testDecodeOverflowCompressed() {
        val channel = EmbeddedChannel(Js5ResponseDecoder())

        assertThrows<DecoderException> {
            channel.writeInbound(wrappedBuffer(2, 0, 3, 1, 0x7F, 0xFF.toByte(), 0xFF.toByte(), 0xF7.toByte()))
        }
    }

    @Test
    fun testDecodeInvalidBlockTrailer() {
        val channel = EmbeddedChannel(Js5ResponseDecoder())

        assertThrows<DecoderException> {
            channel.writeInbound(read("invalid-block-trailer.dat"))
        }
    }

    private fun testDecode(container: String, encoded: String, prefetch: Boolean) {
        val channel = EmbeddedChannel(Js5ResponseDecoder())

        channel.writeInbound(read(encoded))

        read(container).use { data ->
            val expected = Js5Response(prefetch, 2, 3, data)

            channel.readInbound<Js5Response>().use { actual ->
                assertEquals(expected, actual)
            }
        }
    }

    private fun read(name: String): ByteBuf {
        Js5ResponseDecoderTest::class.java.getResourceAsStream(name).use { input ->
            return Unpooled.wrappedBuffer(input.readAllBytes())
        }
    }
}
