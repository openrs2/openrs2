package dev.openrs2.buffer

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

object ByteBufExtensionsTest {
    @Test
    fun testReadShortSmart() {
        Unpooled.wrappedBuffer(byteArrayOf(0x00)).use { buf ->
            assertEquals(-0x40, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x40)).use { buf ->
            assertEquals(0, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x7F)).use { buf ->
            assertEquals(0x3F, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x80.toByte(), 0x00)).use { buf ->
            assertEquals(-0x4000, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xBF.toByte(), 0xBF.toByte())).use { buf ->
            assertEquals(-0x41, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xC0.toByte(), 0x40.toByte())).use { buf ->
            assertEquals(0x40, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xFF.toByte(), 0xFF.toByte())).use { buf ->
            assertEquals(0x3FFF, buf.readShortSmart())
            assertFalse(buf.isReadable)
        }
    }

    @Test
    fun testWriteShortSmart() {
        Unpooled.wrappedBuffer(byteArrayOf(0x00)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeShortSmart(-0x40)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x40)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeShortSmart(0)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x7F)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeShortSmart(0x3F)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x80.toByte(), 0x00)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeShortSmart(-0x4000)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xBF.toByte(), 0xBF.toByte())).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeShortSmart(-0x41)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xC0.toByte(), 0x40.toByte())).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeShortSmart(0x40)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xFF.toByte(), 0xFF.toByte())).use { expected ->
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
        Unpooled.wrappedBuffer(byteArrayOf(0x00)).use { buf ->
            assertEquals(0, buf.readUnsignedShortSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x40)).use { buf ->
            assertEquals(0x40, buf.readUnsignedShortSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x7F)).use { buf ->
            assertEquals(0x7F, buf.readUnsignedShortSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x80.toByte(), 0x80.toByte())).use { buf ->
            assertEquals(0x80, buf.readUnsignedShortSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xC0.toByte(), 0x00)).use { buf ->
            assertEquals(0x4000, buf.readUnsignedShortSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xFF.toByte(), 0xFF.toByte())).use { buf ->
            assertEquals(0x7FFF, buf.readUnsignedShortSmart())
            assertFalse(buf.isReadable)
        }
    }

    @Test
    fun testWriteUnsignedShortSmart() {
        Unpooled.wrappedBuffer(byteArrayOf(0x00)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedShortSmart(0)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x40)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedShortSmart(0x40)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x7F)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedShortSmart(0x7F)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x80.toByte(), 0x80.toByte())).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedShortSmart(0x80)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xC0.toByte(), 0x00)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedShortSmart(0x4000)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xFF.toByte(), 0xFF.toByte())).use { expected ->
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
        Unpooled.wrappedBuffer(byteArrayOf(0x00, 0x00)).use { buf ->
            assertEquals(-0x4000, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x40, 0x00)).use { buf ->
            assertEquals(0, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x7F, 0xFF.toByte())).use { buf ->
            assertEquals(0x3FFF, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x80.toByte(), 0x00, 0x00, 0x00)).use { buf ->
            assertEquals(-0x40000000, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xBF.toByte(), 0xFF.toByte(), 0xBF.toByte(), 0xFF.toByte())).use { buf ->
            assertEquals(-0x4001, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xC0.toByte(), 0x00, 0x40.toByte(), 0x00)).use { buf ->
            assertEquals(0x4000, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())).use { buf ->
            assertEquals(0x3FFFFFFF, buf.readIntSmart())
            assertFalse(buf.isReadable)
        }
    }

    @Test
    fun testWriteIntSmart() {
        Unpooled.wrappedBuffer(byteArrayOf(0x00, 0x00)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeIntSmart(-0x4000)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x40, 0x00)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeIntSmart(0)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x7F, 0xFF.toByte())).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeIntSmart(0x3FFF)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x80.toByte(), 0x00, 0x00, 0x00)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeIntSmart(-0x40000000)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(
            byteArrayOf(0xBF.toByte(), 0xFF.toByte(), 0xBF.toByte(), 0xFF.toByte())
        ).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeIntSmart(-0x4001)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xC0.toByte(), 0x00, 0x40.toByte(), 0x00)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeIntSmart(0x4000)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        ).use { expected ->
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
        Unpooled.wrappedBuffer(byteArrayOf(0x00, 0x00)).use { buf ->
            assertEquals(0, buf.readUnsignedIntSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x40, 0x00)).use { buf ->
            assertEquals(0x4000, buf.readUnsignedIntSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x7F, 0xFF.toByte())).use { buf ->
            assertEquals(0x7FFF, buf.readUnsignedIntSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x80.toByte(), 0x00, 0x80.toByte(), 0x00)).use { buf ->
            assertEquals(0x8000, buf.readUnsignedIntSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xC0.toByte(), 0x00, 0x00, 0x00)).use { buf ->
            assertEquals(0x40000000, buf.readUnsignedIntSmart())
            assertFalse(buf.isReadable)
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())).use { buf ->
            assertEquals(0x7FFFFFFF, buf.readUnsignedIntSmart())
            assertFalse(buf.isReadable)
        }
    }

    @Test
    fun testWriteUnsignedIntSmart() {
        Unpooled.wrappedBuffer(byteArrayOf(0x00, 0x00)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedIntSmart(0)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x40, 0x00)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedIntSmart(0x4000)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x7F, 0xFF.toByte())).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedIntSmart(0x7FFF)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0x80.toByte(), 0x00, 0x80.toByte(), 0x00)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedIntSmart(0x8000)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(byteArrayOf(0xC0.toByte(), 0x00, 0x00, 0x00)).use { expected ->
            ByteBufAllocator.DEFAULT.buffer().use { actual ->
                actual.writeUnsignedIntSmart(0x40000000)
                assertEquals(expected, actual)
            }
        }

        Unpooled.wrappedBuffer(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        ).use { expected ->
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
}
