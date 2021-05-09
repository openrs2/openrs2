package org.openrs2.buffer

import io.netty.buffer.ByteBufAllocator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BitBufTest {
    @Test
    fun testClear() {
        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            buf.writeInt(1234567890)
            buf.skipBytes(1)

            BitBuf(buf).use { bitBuf ->
                assertEquals(8, bitBuf.readerIndex())
                assertEquals(32, bitBuf.writerIndex())

                bitBuf.clear()

                assertEquals(0, bitBuf.readerIndex())
                assertEquals(0, bitBuf.writerIndex())
            }

            assertEquals(0, buf.readerIndex())
            assertEquals(0, buf.writerIndex())
        }
    }

    @Test
    fun testReadAlignment() {
        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            buf.writeByte(0xFF)

            BitBuf(buf).use { bitBuf ->
                assertEquals(0, bitBuf.readerIndex())
                assertEquals(0x7F, bitBuf.readBits(7))
                assertEquals(7, bitBuf.readerIndex())
                assertEquals(0, buf.readerIndex())
            }

            assertEquals(1, buf.readerIndex())
        }
    }

    @Test
    fun testWriteAlignment() {
        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            buf.setByte(0, 0xFF)

            BitBuf(buf).use { bitBuf ->
                assertEquals(0, bitBuf.writerIndex())
                bitBuf.writeBits(7, 0x7F)
                assertEquals(7, bitBuf.writerIndex())
                assertEquals(0, buf.writerIndex())
            }

            assertEquals(1, buf.writerIndex())
            assertEquals(0xFE, buf.getUnsignedByte(0))
        }
    }

    @Test
    fun testSkipBits() {
        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            buf.writeInt(1234567890)

            BitBuf(buf).use { bitBuf ->
                assertFailsWith<IllegalArgumentException> {
                    bitBuf.skipBits(-1)
                }

                assertEquals(0, bitBuf.readerIndex())
                assertEquals(0, buf.readerIndex())

                bitBuf.skipBits(0)

                assertEquals(0, bitBuf.readerIndex())
                assertEquals(0, buf.readerIndex())

                bitBuf.skipBits(7)

                assertEquals(7, bitBuf.readerIndex())
                assertEquals(0, buf.readerIndex())

                bitBuf.skipBits(2)

                assertEquals(9, bitBuf.readerIndex())
                assertEquals(1, buf.readerIndex())

                bitBuf.skipBits(23)

                assertEquals(32, bitBuf.readerIndex())
                assertEquals(4, buf.readerIndex())

                bitBuf.skipBits(0)

                assertEquals(32, bitBuf.readerIndex())
                assertEquals(4, buf.readerIndex())

                assertFailsWith<IndexOutOfBoundsException> {
                    bitBuf.skipBits(1)
                }
            }

            assertEquals(4, buf.readerIndex())
        }
    }

    @Test
    fun testGetBits() {
        ByteBufAllocator.DEFAULT.buffer(4, 4).use { buf ->
            buf.writeInt(1234567890)

            BitBuf(buf).use { bitBuf ->
                assertEquals(1234567890, bitBuf.getBits(0, 32))

                assertEquals(0b0100100110010110, bitBuf.getBits(0, 16))
                assertEquals(0b0000001011010010, bitBuf.getBits(16, 16))

                assertEquals(0b01001001, bitBuf.getBits(0, 8))
                assertEquals(0b10010110, bitBuf.getBits(8, 8))
                assertEquals(0b00000010, bitBuf.getBits(16, 8))
                assertEquals(0b11010010, bitBuf.getBits(24, 8))

                assertEquals(0b10011001, bitBuf.getBits(4, 8))

                assertEquals(0b100110010110, bitBuf.getBits(4, 12))

                assertEquals(0, bitBuf.getBits(0, 1))
                assertEquals(0, bitBuf.getBit(0))
                assertEquals(false, bitBuf.getBoolean(0))

                assertEquals(1, bitBuf.getBits(1, 1))
                assertEquals(1, bitBuf.getBit(1))
                assertEquals(true, bitBuf.getBoolean(1))

                assertFailsWith<IndexOutOfBoundsException> {
                    bitBuf.getBits(-1, 1)
                }

                assertFailsWith<IndexOutOfBoundsException> {
                    bitBuf.getBits(32, 1)
                }

                assertFailsWith<IllegalArgumentException> {
                    bitBuf.getBits(0, 0)
                }

                assertFailsWith<IllegalArgumentException> {
                    bitBuf.getBits(0, 33)
                }
            }
        }
    }

    @Test
    fun testSetBits() {
        ByteBufAllocator.DEFAULT.buffer(4, 4).use { buf ->
            BitBuf(buf).use { bitBuf ->
                bitBuf.setBits(0, 32, 1234567890)
            }

            assertEquals(1234567890, buf.getInt(0))
        }

        ByteBufAllocator.DEFAULT.buffer(4, 4).use { buf ->
            BitBuf(buf).use { bitBuf ->
                bitBuf.setBits(0, 16, 0b0100100110010110)
                bitBuf.setBits(16, 16, 0b0000001011010010)
            }

            assertEquals(1234567890, buf.getInt(0))
        }

        ByteBufAllocator.DEFAULT.buffer(4, 4).use { buf ->
            BitBuf(buf).use { bitBuf ->
                bitBuf.setBits(0, 8, 0b01001001)
                bitBuf.setBits(8, 8, 0b10010110)
                bitBuf.setBits(16, 8, 0b00000010)
                bitBuf.setBits(24, 8, 0b11010010)
            }

            assertEquals(1234567890, buf.getInt(0))
        }

        ByteBufAllocator.DEFAULT.buffer(4, 4).use { buf ->
            BitBuf(buf).use { bitBuf ->
                bitBuf.setBits(0, 4, 0b0100)
                bitBuf.setBits(4, 8, 0b10011001)
                bitBuf.setBits(12, 4, 0b0110)

                bitBuf.setBits(16, 16, 0b0000001011000101)

                bitBuf.setBoolean(27, true)
                bitBuf.setBits(29, 1, 0)
                bitBuf.setBit(30, 1)
                bitBuf.setBoolean(31, false)
            }

            assertEquals(1234567890, buf.getInt(0))
        }

        ByteBufAllocator.DEFAULT.buffer(4, 4).use { buf ->
            BitBuf(buf).use { bitBuf ->
                assertFailsWith<IndexOutOfBoundsException> {
                    bitBuf.setBits(-1, 1, 0)
                }

                assertFailsWith<IndexOutOfBoundsException> {
                    bitBuf.setBits(32, 1, 0)
                }

                assertFailsWith<IllegalArgumentException> {
                    bitBuf.setBits(0, 0, 0)
                }

                assertFailsWith<IllegalArgumentException> {
                    bitBuf.setBits(0, 33, 0)
                }
            }
        }
    }

    @Test
    fun testRead() {
        ByteBufAllocator.DEFAULT.buffer(4, 4).use { buf ->
            buf.writeInt(1234567890)

            BitBuf(buf).use { bitBuf ->
                assertEquals(0, bitBuf.readerIndex())
                assertEquals(32, bitBuf.readableBits())

                assertTrue(bitBuf.isReadable())
                assertTrue(bitBuf.isReadable(0))
                assertTrue(bitBuf.isReadable(1))
                assertTrue(bitBuf.isReadable(31))
                assertTrue(bitBuf.isReadable(32))
                assertFalse(bitBuf.isReadable(33))

                assertEquals(1234567890, bitBuf.readBits(32))
                assertEquals(32, bitBuf.readerIndex())
                assertEquals(0, bitBuf.readableBits())

                assertFalse(bitBuf.isReadable())
                assertTrue(bitBuf.isReadable(0))
                assertFalse(bitBuf.isReadable(1))
            }

            assertEquals(4, buf.readerIndex())
        }

        ByteBufAllocator.DEFAULT.buffer(4, 4).use { buf ->
            buf.writeInt(1234567890)

            BitBuf(buf).use { bitBuf ->
                assertEquals(0, bitBuf.readerIndex())
                assertEquals(32, bitBuf.readableBits())

                assertEquals(0b0100100110010110, bitBuf.readBits(16))

                assertEquals(16, bitBuf.readerIndex())
                assertEquals(16, bitBuf.readableBits())

                assertEquals(0b0000001011010010, bitBuf.readBits(16))

                assertEquals(32, bitBuf.readerIndex())
                assertEquals(0, bitBuf.readableBits())
            }

            assertEquals(4, buf.readerIndex())
        }

        ByteBufAllocator.DEFAULT.buffer(4, 4).use { buf ->
            buf.writeInt(1234567890)

            BitBuf(buf).use { bitBuf ->
                assertEquals(0, bitBuf.readerIndex())
                assertEquals(32, bitBuf.readableBits())

                assertEquals(0b01001001, bitBuf.readBits(8))

                assertEquals(8, bitBuf.readerIndex())
                assertEquals(24, bitBuf.readableBits())

                assertEquals(0b10010110, bitBuf.readBits(8))

                assertEquals(16, bitBuf.readerIndex())
                assertEquals(16, bitBuf.readableBits())

                assertEquals(0b00000010, bitBuf.readBits(8))

                assertEquals(24, bitBuf.readerIndex())
                assertEquals(8, bitBuf.readableBits())

                assertEquals(0b11010010, bitBuf.readBits(8))

                assertEquals(32, bitBuf.readerIndex())
                assertEquals(0, bitBuf.readableBits())
            }

            assertEquals(4, buf.readerIndex())
        }

        ByteBufAllocator.DEFAULT.buffer(1, 1).use { buf ->
            buf.writeByte(0b01010100)

            BitBuf(buf).use { bitBuf ->
                assertEquals(0, bitBuf.readerIndex())
                assertEquals(8, bitBuf.readableBits())

                assertEquals(0, bitBuf.readBits(1))

                assertEquals(1, bitBuf.readerIndex())
                assertEquals(7, bitBuf.readableBits())

                assertEquals(1, bitBuf.readBits(1))

                assertEquals(2, bitBuf.readerIndex())
                assertEquals(6, bitBuf.readableBits())

                assertEquals(0, bitBuf.readBit())

                assertEquals(3, bitBuf.readerIndex())
                assertEquals(5, bitBuf.readableBits())

                assertEquals(1, bitBuf.readBit())

                assertEquals(4, bitBuf.readerIndex())
                assertEquals(4, bitBuf.readableBits())

                assertFalse(bitBuf.readBoolean())

                assertEquals(5, bitBuf.readerIndex())
                assertEquals(3, bitBuf.readableBits())

                assertTrue(bitBuf.readBoolean())

                assertEquals(6, bitBuf.readerIndex())
                assertEquals(2, bitBuf.readableBits())

                bitBuf.skipBits(2)

                assertEquals(8, bitBuf.readerIndex())
                assertEquals(0, bitBuf.readableBits())
            }

            assertEquals(1, buf.readerIndex())
        }

        ByteBufAllocator.DEFAULT.buffer(5, 5).use { buf ->
            buf.writerIndex(5)

            BitBuf(buf).use { bitBuf ->
                assertFailsWith<IllegalArgumentException> {
                    bitBuf.readBits(0)
                }

                assertFailsWith<IllegalArgumentException> {
                    bitBuf.readBits(33)
                }
            }
        }
    }

    @Test
    fun testWrite() {
        ByteBufAllocator.DEFAULT.buffer(4, 4).use { buf ->
            BitBuf(buf).use { bitBuf ->
                assertEquals(0, bitBuf.writerIndex())
                assertEquals(32, bitBuf.writableBits())

                assertTrue(bitBuf.isWritable())
                assertTrue(bitBuf.isWritable(0))
                assertTrue(bitBuf.isWritable(1))
                assertTrue(bitBuf.isWritable(31))
                assertTrue(bitBuf.isWritable(32))
                assertFalse(bitBuf.isWritable(33))

                bitBuf.writeBits(32, 1234567890)

                assertEquals(32, bitBuf.writerIndex())
                assertEquals(0, bitBuf.writableBits())

                assertFalse(bitBuf.isWritable())
                assertTrue(bitBuf.isWritable(0))
                assertFalse(bitBuf.isWritable(1))
            }

            assertEquals(1234567890, buf.getInt(0))
            assertEquals(4, buf.writerIndex())
        }

        ByteBufAllocator.DEFAULT.buffer(4, 4).use { buf ->
            BitBuf(buf).use { bitBuf ->
                assertEquals(0, bitBuf.writerIndex())
                assertEquals(32, bitBuf.writableBits())

                bitBuf.writeBits(16, 0b0100100110010110)

                assertEquals(16, bitBuf.writerIndex())
                assertEquals(16, bitBuf.writableBits())

                bitBuf.writeBits(16, 0b0000001011010010)

                assertEquals(32, bitBuf.writerIndex())
                assertEquals(0, bitBuf.writableBits())
            }

            assertEquals(1234567890, buf.getInt(0))
            assertEquals(4, buf.writerIndex())
        }

        ByteBufAllocator.DEFAULT.buffer(4, 4).use { buf ->
            BitBuf(buf).use { bitBuf ->
                assertEquals(0, bitBuf.writerIndex())
                assertEquals(32, bitBuf.writableBits())

                bitBuf.writeBits(8, 0b01001001)

                assertEquals(8, bitBuf.writerIndex())
                assertEquals(24, bitBuf.writableBits())

                bitBuf.writeBits(8, 0b10010110)

                assertEquals(16, bitBuf.writerIndex())
                assertEquals(16, bitBuf.writableBits())

                bitBuf.writeBits(8, 0b00000010)

                assertEquals(24, bitBuf.writerIndex())
                assertEquals(8, bitBuf.writableBits())

                bitBuf.writeBits(8, 0b11010010)

                assertEquals(32, bitBuf.writerIndex())
                assertEquals(0, bitBuf.writableBits())
            }

            assertEquals(1234567890, buf.getInt(0))
            assertEquals(4, buf.writerIndex())
        }

        ByteBufAllocator.DEFAULT.buffer(1, 1).use { buf ->
            BitBuf(buf).use { bitBuf ->
                assertEquals(0, bitBuf.writerIndex())
                assertEquals(8, bitBuf.writableBits())

                bitBuf.writeBits(1, 0)

                assertEquals(1, bitBuf.writerIndex())
                assertEquals(7, bitBuf.writableBits())

                bitBuf.writeBits(1, 1)

                assertEquals(2, bitBuf.writerIndex())
                assertEquals(6, bitBuf.writableBits())

                bitBuf.writeBit(0)

                assertEquals(3, bitBuf.writerIndex())
                assertEquals(5, bitBuf.writableBits())

                bitBuf.writeBit(1)

                assertEquals(4, bitBuf.writerIndex())
                assertEquals(4, bitBuf.writableBits())

                bitBuf.writeBoolean(false)

                assertEquals(5, bitBuf.writerIndex())
                assertEquals(3, bitBuf.writableBits())

                bitBuf.writeBoolean(true)

                assertEquals(6, bitBuf.writerIndex())
                assertEquals(2, bitBuf.writableBits())

                bitBuf.writeZero(2)

                assertEquals(8, bitBuf.writerIndex())
                assertEquals(0, bitBuf.writableBits())
            }

            assertEquals(0b01010100, buf.getUnsignedByte(0))
            assertEquals(1, buf.writerIndex())
        }

        ByteBufAllocator.DEFAULT.buffer(5, 5).use { buf ->
            BitBuf(buf).use { bitBuf ->
                assertFailsWith<IllegalArgumentException> {
                    bitBuf.writeBits(0, 0)
                }

                assertFailsWith<IllegalArgumentException> {
                    bitBuf.writeBits(33, 0)
                }

                assertFailsWith<IllegalArgumentException> {
                    bitBuf.isWritable(-1)
                }
            }
        }
    }

    @Test
    fun testExpand() {
        ByteBufAllocator.DEFAULT.buffer(1, 2).use { buf ->
            BitBuf(buf).use { bitBuf ->
                assertEquals(8, bitBuf.capacity())
                assertEquals(8, bitBuf.writableBits())

                assertEquals(16, bitBuf.maxCapacity())
                assertEquals(16, bitBuf.maxWritableBits())

                bitBuf.writeBits(8, 0)

                assertEquals(0, bitBuf.writableBits())
                assertEquals(8, bitBuf.maxWritableBits())

                bitBuf.writeBit(0)

                assertEquals(7, bitBuf.writableBits())
                assertEquals(7, bitBuf.maxWritableBits())

                bitBuf.writeZero(7)

                assertEquals(0, bitBuf.writableBits())
                assertEquals(0, bitBuf.maxWritableBits())

                assertFailsWith<IndexOutOfBoundsException> {
                    bitBuf.writeBit(0)
                }

                assertFailsWith<IllegalArgumentException> {
                    bitBuf.ensureWritable(-1)
                }
            }
        }
    }

    @Test
    fun testCapacity() {
        ByteBufAllocator.DEFAULT.buffer(4, 16).use { buf ->
            BitBuf(buf).use { bitBuf ->
                assertEquals(32, bitBuf.capacity())
                assertEquals(4, buf.capacity())

                assertEquals(128, bitBuf.maxCapacity())
                assertEquals(16, buf.maxCapacity())

                bitBuf.capacity(64)

                assertEquals(64, bitBuf.capacity())
                assertEquals(8, buf.capacity())
            }
        }
    }

    @Test
    fun testReaderIndex() {
        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            BitBuf(buf).use { bitBuf ->
                bitBuf.writerIndex(10)

                assertEquals(0, bitBuf.readerIndex())

                assertFailsWith<IndexOutOfBoundsException> {
                    bitBuf.readerIndex(-1)
                }

                bitBuf.readerIndex(1)
                assertEquals(1, bitBuf.readerIndex())

                bitBuf.readerIndex(8)
                assertEquals(8, bitBuf.readerIndex())

                bitBuf.readerIndex(9)
                assertEquals(9, bitBuf.readerIndex())

                bitBuf.readerIndex(10)
                assertEquals(10, bitBuf.readerIndex())

                assertFailsWith<IndexOutOfBoundsException> {
                    bitBuf.readerIndex(11)
                }
            }
        }
    }

    @Test
    fun testWriterIndex() {
        ByteBufAllocator.DEFAULT.buffer(4, 4).use { buf ->
            BitBuf(buf).use { bitBuf ->
                bitBuf.writerIndex(20)
                bitBuf.readerIndex(10)

                assertEquals(20, bitBuf.writerIndex())

                assertFailsWith<IndexOutOfBoundsException> {
                    bitBuf.writerIndex(9)
                }

                bitBuf.writerIndex(10)
                assertEquals(10, bitBuf.writerIndex())

                bitBuf.writerIndex(11)
                assertEquals(11, bitBuf.writerIndex())

                bitBuf.writerIndex(31)
                assertEquals(31, bitBuf.writerIndex())

                bitBuf.writerIndex(32)
                assertEquals(32, bitBuf.writerIndex())

                assertFailsWith<IndexOutOfBoundsException> {
                    bitBuf.writerIndex(33)
                }
            }
        }
    }
}
