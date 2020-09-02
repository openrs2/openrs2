package dev.openrs2.cache

import dev.openrs2.buffer.use
import io.netty.buffer.Unpooled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

object VersionTrailerTest {
    @Test
    public fun testStrip() {
        assertNull(VersionTrailer.strip(Unpooled.EMPTY_BUFFER))

        Unpooled.wrappedBuffer(byteArrayOf(0)).use { buf ->
            assertNull(VersionTrailer.strip(buf))

            Unpooled.wrappedBuffer(byteArrayOf(0)).use { expected ->
                assertEquals(expected, buf)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x12, 0x34)).use { buf ->
            assertEquals(0x1234, VersionTrailer.strip(buf))
            assertEquals(Unpooled.EMPTY_BUFFER, buf)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x12, 0x34, 0x56)).use { buf ->
            assertEquals(0x3456, VersionTrailer.strip(buf))

            Unpooled.wrappedBuffer(byteArrayOf(0x012)).use { expected ->
                assertEquals(expected, buf)
            }
        }
    }
}
