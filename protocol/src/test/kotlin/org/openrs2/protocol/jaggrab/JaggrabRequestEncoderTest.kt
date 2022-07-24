package org.openrs2.protocol.jaggrab

import io.netty.channel.embedded.EmbeddedChannel
import org.openrs2.protocol.jaggrab.upstream.JaggrabRequest
import org.openrs2.protocol.jaggrab.upstream.JaggrabRequestEncoder
import kotlin.test.Test
import kotlin.test.assertEquals

class JaggrabRequestEncoderTest {
    @Test
    fun testEncode() {
        val channel = EmbeddedChannel(JaggrabRequestEncoder)
        channel.writeOutbound(JaggrabRequest("/runescape.pack200"))

        val actual = channel.readOutbound<String>()
        assertEquals("JAGGRAB /runescape.pack200\n\n", actual)
    }
}
