package org.openrs2.protocol.js5

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.EncoderException
import org.openrs2.buffer.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Js5ResponseEncoderTest {
    @Test
    fun testEncode() {
        testEncode("509.dat", "509-prefetch.dat", true)
        testEncode("509.dat", "509-urgent.dat", false)

        testEncode("510.dat", "510-prefetch.dat", true)
        testEncode("510.dat", "510-urgent.dat", false)

        testEncode("1020.dat", "1020-prefetch.dat", true)
        testEncode("1020.dat", "1020-urgent.dat", false)

        testEncode("1021.dat", "1021-prefetch.dat", true)
        testEncode("1021.dat", "1021-urgent.dat", false)

        testEncode("1531.dat", "1531-prefetch.dat", true)
        testEncode("1531.dat", "1531-urgent.dat", false)

        testEncode("1532.dat", "1532-prefetch.dat", true)
        testEncode("1532.dat", "1532-urgent.dat", false)
    }

    @Test
    fun testEncodeEmpty() {
        val channel = EmbeddedChannel(Js5ResponseEncoder)

        assertFailsWith<EncoderException> {
            channel.writeOutbound(Js5Response(true, 2, 3, Unpooled.EMPTY_BUFFER))
        }
    }

    private fun testEncode(container: String, encoded: String, prefetch: Boolean) {
        val channel = EmbeddedChannel(Js5ResponseEncoder)

        read(container).use { buf ->
            channel.writeOutbound(Js5Response(prefetch, 2, 3, buf.retain()))
        }

        read(encoded).use { expected ->
            channel.readOutbound<ByteBuf>().use { actual ->
                assertEquals(expected, actual)
            }
        }
    }

    private fun read(name: String): ByteBuf {
        Js5ResponseEncoderTest::class.java.getResourceAsStream(name).use { input ->
            return Unpooled.wrappedBuffer(input.readAllBytes())
        }
    }
}
