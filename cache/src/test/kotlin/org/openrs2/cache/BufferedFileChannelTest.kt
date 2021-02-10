package org.openrs2.cache

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.openrs2.buffer.copiedBuffer
import org.openrs2.buffer.use
import org.openrs2.buffer.wrappedBuffer
import java.io.EOFException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BufferedFileChannelTest {
    @Test
    fun testEmpty() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                assertEquals(0, channel.size())
            }
        }
    }

    @Test
    fun testBufferedWrite() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                copiedBuffer("OpenRS2").use { buf ->
                    channel.write(0, buf, buf.readableBytes())
                }

                assertEquals(7, channel.size())
            }

            Unpooled.wrappedBuffer(Files.readAllBytes(path)).use { actual ->
                copiedBuffer("OpenRS2").use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testBufferedWriteOverlapStart() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                copiedBuffer("OpenRS2").use { buf ->
                    channel.write(1, buf.slice(), buf.readableBytes())
                    channel.write(0, buf.slice(), buf.readableBytes())
                }
            }

            Unpooled.wrappedBuffer(Files.readAllBytes(path)).use { actual ->
                copiedBuffer("OpenRS22").use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testBufferedWriteOverlapEnd() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                copiedBuffer("OpenRS2").use { buf ->
                    channel.write(0, buf.slice(), buf.readableBytes())
                    channel.write(1, buf.slice(), buf.readableBytes())
                }
            }

            Unpooled.wrappedBuffer(Files.readAllBytes(path)).use { actual ->
                copiedBuffer("OOpenRS2").use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testBufferedWriteAdjacent() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                copiedBuffer("OpenRS2").use { buf ->
                    channel.write(0, buf.slice(), buf.readableBytes())
                    channel.write(7, buf.slice(), buf.readableBytes())
                }
            }

            Unpooled.wrappedBuffer(Files.readAllBytes(path)).use { actual ->
                copiedBuffer("OpenRS2OpenRS2").use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testBufferedWriteNoOverlap() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                copiedBuffer("OpenRS2").use { buf ->
                    channel.write(0, buf.slice(), buf.readableBytes())
                    channel.write(8, buf.slice(), buf.readableBytes())
                }
            }

            Unpooled.wrappedBuffer(Files.readAllBytes(path)).use { actual ->
                copiedBuffer("OpenRS2\u0000OpenRS2").use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testUnbufferedWrite() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                copiedBuffer("Hello, world!").use { buf ->
                    channel.write(0, buf, buf.readableBytes())
                }

                assertEquals(13, channel.size())
            }

            Unpooled.wrappedBuffer(Files.readAllBytes(path)).use { actual ->
                copiedBuffer("Hello, world!").use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testBufferedRead() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")
            Files.write(path, "OpenRS2OpenRS2".toByteArray())

            BufferedFileChannel(FileChannel.open(path, READ), 8, 8).use { channel ->
                ByteBufAllocator.DEFAULT.buffer(7, 7).use { actual ->
                    copiedBuffer("OpenRS2").use { expected ->
                        channel.read(0, actual, actual.writableBytes())
                        assertEquals(expected, actual)

                        actual.clear()

                        channel.read(7, actual, actual.writableBytes())
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testUnbufferedRead() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")
            Files.write(path, "OpenRS2OpenRS2".toByteArray())

            BufferedFileChannel(FileChannel.open(path, READ), 8, 8).use { channel ->
                ByteBufAllocator.DEFAULT.buffer(14, 14).use { actual ->
                    copiedBuffer("OpenRS2OpenRS2").use { expected ->
                        channel.read(0, actual, actual.writableBytes())
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testBufferedEof() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                ByteBufAllocator.DEFAULT.buffer(1, 1).use { buf ->
                    assertFailsWith<EOFException> {
                        channel.read(0, buf, buf.writableBytes())
                    }
                }
            }
        }
    }

    @Test
    fun testUnbufferedEof() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")
            Files.write(path, "OpenRS2OpenRS2".toByteArray())

            BufferedFileChannel(FileChannel.open(path, READ), 8, 8).use { channel ->
                ByteBufAllocator.DEFAULT.buffer(15, 15).use { buf ->
                    assertFailsWith<EOFException> {
                        channel.read(0, buf, buf.writableBytes())
                    }
                }
            }
        }
    }

    @Test
    fun testZeroExtension() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                wrappedBuffer(1).use { buf ->
                    channel.write(7, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(8, 8).use { actual ->
                    channel.read(0, actual, actual.writableBytes())

                    wrappedBuffer(0, 0, 0, 0, 0, 0, 0, 1).use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testBufferedWriteThenRead() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                copiedBuffer("OpenRS2").use { buf ->
                    channel.write(0, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(7, 7).use { actual ->
                    channel.read(0, actual, actual.writableBytes())

                    copiedBuffer("OpenRS2").use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testBufferedWriteThenReadSubsetStart() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                copiedBuffer("OpenRS2").use { buf ->
                    channel.write(0, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(6, 6).use { actual ->
                    channel.read(0, actual, actual.writableBytes())

                    copiedBuffer("OpenRS").use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testBufferedWriteThenReadSubsetEnd() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                copiedBuffer("OpenRS2").use { buf ->
                    channel.write(0, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(6, 6).use { actual ->
                    channel.read(1, actual, actual.writableBytes())

                    copiedBuffer("penRS2").use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testBufferedWriteThenReadSuperset() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            Files.write(path, "OpenRS2OpenRS2".toByteArray())

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                copiedBuffer("Hello").use { buf ->
                    channel.write(4, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(14, 14).use { actual ->
                    channel.read(0, actual, actual.writableBytes())

                    copiedBuffer("OpenHelloenRS2").use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testBufferedWriteThenReadSupersetStart() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            Files.write(path, "OpenRS2OpenRS2".toByteArray())

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                copiedBuffer("Hello").use { buf ->
                    channel.write(4, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(7, 7).use { actual ->
                    channel.read(0, actual, actual.writableBytes())

                    copiedBuffer("OpenHel").use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testBufferedWriteThenReadSupersetEnd() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            Files.write(path, "OpenRS2OpenRS2".toByteArray())

            BufferedFileChannel(FileChannel.open(path, CREATE, READ, WRITE), 8, 8).use { channel ->
                copiedBuffer("Hello").use { buf ->
                    channel.write(4, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(7, 7).use { actual ->
                    channel.read(7, actual, actual.writableBytes())

                    copiedBuffer("loenRS2").use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testBufferedWriteThenReadNoOverlap() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            Files.write(path, "OpenRS2OpenRS2".toByteArray())

            BufferedFileChannel(FileChannel.open(path, READ, WRITE), 4, 8).use { channel ->
                copiedBuffer("Hello").use { buf ->
                    channel.write(4, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(0, actual, actual.writableBytes())

                    copiedBuffer("Open").use { expected ->
                        assertEquals(expected, actual)
                    }
                }

                ByteBufAllocator.DEFAULT.buffer(5, 5).use { actual ->
                    channel.read(9, actual, actual.writableBytes())

                    copiedBuffer("enRS2").use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testBufferedReadThenWrite() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            Files.write(path, "OpenRS2OpenRS2".toByteArray())

            BufferedFileChannel(FileChannel.open(path, READ, WRITE), 4, 0).use { channel ->
                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    copiedBuffer("RS2O").use { expected ->
                        assertEquals(expected, actual)
                    }
                }

                copiedBuffer("ABCD").use { buf ->
                    channel.write(4, buf.slice(), buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    copiedBuffer("ABCD").use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testBufferedReadThenWriteSubsetStart() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            Files.write(path, "OpenRS2OpenRS2".toByteArray())

            BufferedFileChannel(FileChannel.open(path, READ, WRITE), 4, 0).use { channel ->
                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    copiedBuffer("RS2O").use { expected ->
                        assertEquals(expected, actual)
                    }
                }

                copiedBuffer("ABC").use { buf ->
                    channel.write(4, buf.slice(), buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    copiedBuffer("ABCO").use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testBufferedReadThenWriteSubsetEnd() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            Files.write(path, "OpenRS2OpenRS2".toByteArray())

            BufferedFileChannel(FileChannel.open(path, READ, WRITE), 4, 0).use { channel ->
                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    copiedBuffer("RS2O").use { expected ->
                        assertEquals(expected, actual)
                    }
                }

                copiedBuffer("BCD").use { buf ->
                    channel.write(5, buf.slice(), buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    copiedBuffer("RBCD").use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testBufferedReadThenWriteSupersetStart() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            Files.write(path, "OpenRS2OpenRS2".toByteArray())

            BufferedFileChannel(FileChannel.open(path, READ, WRITE), 4, 0).use { channel ->
                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    copiedBuffer("RS2O").use { expected ->
                        assertEquals(expected, actual)
                    }
                }

                copiedBuffer("ZABCD").use { buf ->
                    channel.write(3, buf.slice(), buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    copiedBuffer("ABCD").use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testBufferedReadThenWriteSupersetEnd() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val path = fs.getPath("/test.dat")

            Files.write(path, "OpenRS2OpenRS2".toByteArray())

            BufferedFileChannel(FileChannel.open(path, READ, WRITE), 4, 0).use { channel ->
                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    copiedBuffer("RS2O").use { expected ->
                        assertEquals(expected, actual)
                    }
                }

                copiedBuffer("ABCDZ").use { buf ->
                    channel.write(4, buf.slice(), buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    copiedBuffer("ABCD").use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }
}
