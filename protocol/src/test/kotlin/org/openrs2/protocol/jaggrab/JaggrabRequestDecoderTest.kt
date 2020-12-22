package org.openrs2.protocol.jaggrab

import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.DecoderException
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

object JaggrabRequestDecoderTest {
    @Test
    fun testDecode() {
        val channel = EmbeddedChannel(JaggrabRequestDecoder)
        channel.writeInbound("JAGGRAB /runescape.pack200")

        val actual = channel.readInbound<JaggrabRequest>()
        assertEquals(JaggrabRequest("/runescape.pack200"), actual)
    }

    @Test
    fun testInvalid() {
        val channel = EmbeddedChannel(JaggrabRequestDecoder)

        assertThrows<DecoderException> {
            channel.writeInbound("Hello, world!")
        }
    }
}
