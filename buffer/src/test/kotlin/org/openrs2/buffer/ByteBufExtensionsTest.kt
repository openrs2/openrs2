package org.openrs2.buffer

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

object ByteBufExtensionsTest {
    @Test
    fun testWrappedBuffer() {
        wrappedBuffer(1, 2, 3).use { actual ->
            Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3)).use { expected ->
                assertEquals(expected, actual)
            }
        }

        copiedBuffer("ØpenRS2").use { actual ->
            Unpooled.wrappedBuffer("ØpenRS2".toByteArray()).use { expected ->
                assertEquals(expected, actual)
            }
        }

        copiedBuffer("ØpenRS2", Charsets.UTF_16BE).use { actual ->
            Unpooled.wrappedBuffer("ØpenRS2".toByteArray(Charsets.UTF_16BE)).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testReadShortSmart() {
        wrappedBuffer(0x00).use { buf ->
            assertEquals(-0x40, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0x40).use { buf ->
            assertEquals(0, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0x7F).use { buf ->
            assertEquals(0x3F, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0x80.toByte(), 0x00).use { buf ->
            assertEquals(-0x4000, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0xBF.toByte(), 0xBF.toByte()).use { buf ->
            assertEquals(-0x41, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0xC0.toByte(), 0x40.toByte()).use { buf ->
            assertEquals(0x40, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0xFF.toByte(), 0xFF.toByte()).use { buf ->
            assertEquals(0x3FFF, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }
    }

    @Test
    fun testWriteShortSmart() {
        wrappedBuffer(0x00).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeShortSmart(-0x40)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0x40).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeShortSmart(0)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0x7F).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeShortSmart(0x3F)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0x80.toByte(), 0x00).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeShortSmart(-0x4000)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0xBF.toByte(), 0xBF.toByte()).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeShortSmart(-0x41)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0xC0.toByte(), 0x40.toByte()).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeShortSmart(0x40)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0xFF.toByte(), 0xFF.toByte()).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeShortSmart(0x3FFF)
                assertEquals(expected, actual)
            }
        }

        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            assertThrows<IllegalArgumentException> {
                buf.writeShortSmart(-0x4001)
            }
        }

        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            assertThrows<IllegalArgumentException> {
                buf.writeShortSmart(0x4000)
            }
        }
    }

    @Test
    fun testReadUnsignedShortSmart() {
        wrappedBuffer(0x00).use { buf ->
            assertEquals(0, buf.readUnsignedShortSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0x40).use { buf ->
            assertEquals(0x40, buf.readUnsignedShortSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0x7F).use { buf ->
            assertEquals(0x7F, buf.readUnsignedShortSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0x80.toByte(), 0x80.toByte()).use { buf ->
            assertEquals(0x80, buf.readUnsignedShortSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0xC0.toByte(), 0x00).use { buf ->
            assertEquals(0x4000, buf.readUnsignedShortSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0xFF.toByte(), 0xFF.toByte()).use { buf ->
            assertEquals(0x7FFF, buf.readUnsignedShortSmart())
            assertFalse(buf.isReadable)
        }
    }

    @Test
    fun testWriteUnsignedShortSmart() {
        wrappedBuffer(0x00).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedShortSmart(0)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0x40).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedShortSmart(0x40)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0x7F).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedShortSmart(0x7F)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0x80.toByte(), 0x80.toByte()).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedShortSmart(0x80)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0xC0.toByte(), 0x00).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedShortSmart(0x4000)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0xFF.toByte(), 0xFF.toByte()).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedShortSmart(0x7FFF)
                assertEquals(expected, actual)
            }
        }

        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            assertThrows<IllegalArgumentException> {
                buf.writeUnsignedShortSmart(-0x1)
            }
        }

        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            assertThrows<IllegalArgumentException> {
                buf.writeUnsignedShortSmart(0x10000)
            }
        }
    }

    @Test
    fun testReadIntSmart() {
        wrappedBuffer(0x00, 0x00).use { buf ->
            assertEquals(-0x4000, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0x40, 0x00).use { buf ->
            assertEquals(0, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0x7F, 0xFF.toByte()).use { buf ->
            assertEquals(0x3FFF, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0x80.toByte(), 0x00, 0x00, 0x00).use { buf ->
            assertEquals(-0x40000000, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0xBF.toByte(), 0xFF.toByte(), 0xBF.toByte(), 0xFF.toByte()).use { buf ->
            assertEquals(-0x4001, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0xC0.toByte(), 0x00, 0x40.toByte(), 0x00).use { buf ->
            assertEquals(0x4000, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()).use { buf ->
            assertEquals(0x3FFFFFFF, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }
    }

    @Test
    fun testWriteIntSmart() {
        wrappedBuffer(0x00, 0x00).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeIntSmart(-0x4000)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0x40, 0x00).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeIntSmart(0)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0x7F, 0xFF.toByte()).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeIntSmart(0x3FFF)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0x80.toByte(), 0x00, 0x00, 0x00).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeIntSmart(-0x40000000)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0xBF.toByte(), 0xFF.toByte(), 0xBF.toByte(), 0xFF.toByte()).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeIntSmart(-0x4001)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0xC0.toByte(), 0x00, 0x40.toByte(), 0x00).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeIntSmart(0x4000)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeIntSmart(0x3FFFFFFF)
                assertEquals(expected, actual)
            }
        }

        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            assertThrows<IllegalArgumentException> {
                buf.writeIntSmart(-0x40000001)
            }
        }

        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            assertThrows<IllegalArgumentException> {
                buf.writeIntSmart(0x40000000)
            }
        }
    }

    @Test
    fun testReadUnsignedIntSmart() {
        wrappedBuffer(0x00, 0x00).use { buf ->
            assertEquals(0, buf.readUnsignedIntSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0x40, 0x00).use { buf ->
            assertEquals(0x4000, buf.readUnsignedIntSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0x7F, 0xFF.toByte()).use { buf ->
            assertEquals(0x7FFF, buf.readUnsignedIntSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0x80.toByte(), 0x00, 0x80.toByte(), 0x00).use { buf ->
            assertEquals(0x8000, buf.readUnsignedIntSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0xC0.toByte(), 0x00, 0x00, 0x00).use { buf ->
            assertEquals(0x40000000, buf.readUnsignedIntSmart())
            assertFalse(buf.isReadable)
        }

        wrappedBuffer(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()).use { buf ->
            assertEquals(0x7FFFFFFF, buf.readUnsignedIntSmart())
            assertFalse(buf.isReadable)
        }
    }

    @Test
    fun testWriteUnsignedIntSmart() {
        wrappedBuffer(0x00, 0x00).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedIntSmart(0)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0x40, 0x00).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedIntSmart(0x4000)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0x7F, 0xFF.toByte()).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedIntSmart(0x7FFF)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0x80.toByte(), 0x00, 0x80.toByte(), 0x00).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedIntSmart(0x8000)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0xC0.toByte(), 0x00, 0x00, 0x00).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedIntSmart(0x40000000)
                assertEquals(expected, actual)
            }
        }

        wrappedBuffer(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedIntSmart(0x7FFFFFFF)
                assertEquals(expected, actual)
            }
        }

        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            assertThrows<IllegalArgumentException> {
                buf.writeUnsignedIntSmart(0x80000000.toInt())
            }
        }
    }

    @Test
    fun testCrc32() {
        val s = "AAThe quick brown fox jumps over the lazy dogA"

        /*
         * Tests the hasArray() case. The slicedBuf trickery is to allow us
         * to test a non-zero arrayOffset().
         */
        copiedBuffer(s).use { buf ->
            val slicedBuf = buf.slice(1, buf.readableBytes() - 1)
            assertEquals(0x414FA339, slicedBuf.crc32(1, slicedBuf.writerIndex() - 2))
        }

        // Tests the nioBufferCount() == 1 case.
        copiedBuffer(s).use { buf ->
            ByteBufAllocator.DEFAULT.directBuffer().use { directBuf ->
                directBuf.writeBytes(buf)

                assertEquals(0x414FA339, directBuf.crc32(2, directBuf.writerIndex() - 3))
            }
        }

        // Tests the nioBufferCount() > 1 case.
        Unpooled.wrappedBuffer(
            copiedBuffer("AAThe quick brown fox "),
            copiedBuffer("jumps over the lazy dogA")
        ).use { buf ->
            assertEquals(0x414FA339, buf.crc32(2, buf.writerIndex() - 3))
        }

        /*
         * Check the crc32() method (with no arguments) sets the index/length
         * correctly.
         */
        copiedBuffer(s).use { buf ->
            buf.readerIndex(2)
            buf.writerIndex(buf.writerIndex() - 1)
            assertEquals(0x414FA339, buf.crc32())
        }
    }

    @Test
    fun testCrc32Bounds() {
        assertThrows<IndexOutOfBoundsException> {
            Unpooled.EMPTY_BUFFER.crc32(-1, 0)
        }

        assertThrows<IndexOutOfBoundsException> {
            Unpooled.EMPTY_BUFFER.crc32(0, -1)
        }

        assertThrows<IndexOutOfBoundsException> {
            Unpooled.EMPTY_BUFFER.crc32(1, 0)
        }

        assertThrows<IndexOutOfBoundsException> {
            Unpooled.EMPTY_BUFFER.crc32(0, 1)
        }
    }
}
