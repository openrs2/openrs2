package org.openrs2.cache

import io.netty.buffer.Unpooled
import org.openrs2.buffer.use
import org.openrs2.buffer.wrappedBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VersionTrailerTest {
    @Test
    fun testStrip() {
        assertNull(VersionTrailer.strip(Unpooled.EMPTY_BUFFER))

        wrappedBuffer(0).use { buf ->
            assertNull(VersionTrailer.strip(buf))

            wrappedBuffer(0).use { expected ->
                assertEquals(expected, buf)
            }
        }

        wrappedBuffer(0x12, 0x34).use { buf ->
            assertEquals(0x1234, VersionTrailer.strip(buf))
            assertEquals(Unpooled.EMPTY_BUFFER, buf)
        }

        wrappedBuffer(0x12, 0x34, 0x56).use { buf ->
            assertEquals(0x3456, VersionTrailer.strip(buf))

            wrappedBuffer(0x012).use { expected ->
                assertEquals(expected, buf)
            }
        }
    }
}
