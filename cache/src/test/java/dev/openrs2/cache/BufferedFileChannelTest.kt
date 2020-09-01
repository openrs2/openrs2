package dev.openrs2.cache

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import dev.openrs2.buffer.use
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.EOFException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE
import kotlin.test.assertEquals

object BufferedFileChannelTest {
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
                Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { buf ->
                    channel.write(0, buf, buf.readableBytes())
                }

                assertEquals(7, channel.size())
            }

            Unpooled.wrappedBuffer(Files.readAllBytes(path)).use { actual ->
                Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
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
                Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { buf ->
                    channel.write(1, buf.slice(), buf.readableBytes())
                    channel.write(0, buf.slice(), buf.readableBytes())
                }
            }

            Unpooled.wrappedBuffer(Files.readAllBytes(path)).use { actual ->
                Unpooled.wrappedBuffer("OpenRS22".toByteArray()).use { expected ->
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
                Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { buf ->
                    channel.write(0, buf.slice(), buf.readableBytes())
                    channel.write(1, buf.slice(), buf.readableBytes())
                }
            }

            Unpooled.wrappedBuffer(Files.readAllBytes(path)).use { actual ->
                Unpooled.wrappedBuffer("OOpenRS2".toByteArray()).use { expected ->
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
                Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { buf ->
                    channel.write(0, buf.slice(), buf.readableBytes())
                    channel.write(7, buf.slice(), buf.readableBytes())
                }
            }

            Unpooled.wrappedBuffer(Files.readAllBytes(path)).use { actual ->
                Unpooled.wrappedBuffer("OpenRS2OpenRS2".toByteArray()).use { expected ->
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
                Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { buf ->
                    channel.write(0, buf.slice(), buf.readableBytes())
                    channel.write(8, buf.slice(), buf.readableBytes())
                }
            }

            Unpooled.wrappedBuffer(Files.readAllBytes(path)).use { actual ->
                Unpooled.wrappedBuffer("OpenRS2\u0000OpenRS2".toByteArray()).use { expected ->
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
                Unpooled.wrappedBuffer("Hello, world!".toByteArray()).use { buf ->
                    channel.write(0, buf, buf.readableBytes())
                }

                assertEquals(13, channel.size())
            }

            Unpooled.wrappedBuffer(Files.readAllBytes(path)).use { actual ->
                Unpooled.wrappedBuffer("Hello, world!".toByteArray()).use { expected ->
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
                    Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
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
                    Unpooled.wrappedBuffer("OpenRS2OpenRS2".toByteArray()).use { expected ->
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
                    assertThrows<EOFException> {
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
                    assertThrows<EOFException> {
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
                Unpooled.wrappedBuffer(byteArrayOf(1)).use { buf ->
                    channel.write(7, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(8, 8).use { actual ->
                    channel.read(0, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1)).use { expected ->
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
                Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { buf ->
                    channel.write(0, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(7, 7).use { actual ->
                    channel.read(0, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
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
                Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { buf ->
                    channel.write(0, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(6, 6).use { actual ->
                    channel.read(0, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer("OpenRS".toByteArray()).use { expected ->
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
                Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { buf ->
                    channel.write(0, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(6, 6).use { actual ->
                    channel.read(1, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer("penRS2".toByteArray()).use { expected ->
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
                Unpooled.wrappedBuffer("Hello".toByteArray()).use { buf ->
                    channel.write(4, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(14, 14).use { actual ->
                    channel.read(0, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer("OpenHelloenRS2".toByteArray()).use { expected ->
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
                Unpooled.wrappedBuffer("Hello".toByteArray()).use { buf ->
                    channel.write(4, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(7, 7).use { actual ->
                    channel.read(0, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer("OpenHel".toByteArray()).use { expected ->
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
                Unpooled.wrappedBuffer("Hello".toByteArray()).use { buf ->
                    channel.write(4, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(7, 7).use { actual ->
                    channel.read(7, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer("loenRS2".toByteArray()).use { expected ->
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
                Unpooled.wrappedBuffer("Hello".toByteArray()).use { buf ->
                    channel.write(4, buf, buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(0, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer("Open".toByteArray()).use { expected ->
                        assertEquals(expected, actual)
                    }
                }

                ByteBufAllocator.DEFAULT.buffer(5, 5).use { actual ->
                    channel.read(9, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer("enRS2".toByteArray()).use { expected ->
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

                    Unpooled.wrappedBuffer("RS2O".toByteArray()).use { expected ->
                        assertEquals(expected, actual)
                    }
                }

                Unpooled.wrappedBuffer("ABCD".toByteArray()).use { buf ->
                    channel.write(4, buf.slice(), buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer("ABCD".toByteArray()).use { expected ->
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

                    Unpooled.wrappedBuffer("RS2O".toByteArray()).use { expected ->
                        assertEquals(expected, actual)
                    }
                }

                Unpooled.wrappedBuffer("ABC".toByteArray()).use { buf ->
                    channel.write(4, buf.slice(), buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer("ABCO".toByteArray()).use { expected ->
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

                    Unpooled.wrappedBuffer("RS2O".toByteArray()).use { expected ->
                        assertEquals(expected, actual)
                    }
                }

                Unpooled.wrappedBuffer("BCD".toByteArray()).use { buf ->
                    channel.write(5, buf.slice(), buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer("RBCD".toByteArray()).use { expected ->
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

                    Unpooled.wrappedBuffer("RS2O".toByteArray()).use { expected ->
                        assertEquals(expected, actual)
                    }
                }

                Unpooled.wrappedBuffer("ZABCD".toByteArray()).use { buf ->
                    channel.write(3, buf.slice(), buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer("ABCD".toByteArray()).use { expected ->
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

                    Unpooled.wrappedBuffer("RS2O".toByteArray()).use { expected ->
                        assertEquals(expected, actual)
                    }
                }

                Unpooled.wrappedBuffer("ABCDZ".toByteArray()).use { buf ->
                    channel.write(4, buf.slice(), buf.readableBytes())
                }

                ByteBufAllocator.DEFAULT.buffer(4, 4).use { actual ->
                    channel.read(4, actual, actual.writableBytes())

                    Unpooled.wrappedBuffer("ABCD".toByteArray()).use { expected ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }
}
