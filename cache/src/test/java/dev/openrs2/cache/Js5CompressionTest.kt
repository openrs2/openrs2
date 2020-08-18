package dev.openrs2.cache

import dev.openrs2.buffer.use
import dev.openrs2.crypto.XteaKey
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

object Js5CompressionTest {
    private val KEY = XteaKey.fromHex("00112233445566778899AABBCCDDEEFF")

    @Test
    fun testCompressNone() {
        read("none.dat").use { expected ->
            Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { input ->
                Js5Compression.compress(input, Js5CompressionType.NONE).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testUncompressNone() {
        Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
            read("none.dat").use { input ->
                Js5Compression.uncompress(input).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testCompressGzip() {
        Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
            Js5Compression.compress(expected.slice(), Js5CompressionType.GZIP).use { compressed ->
                Js5Compression.uncompress(compressed).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testUncompressGzip() {
        Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
            read("gzip.dat").use { input ->
                Js5Compression.uncompress(input).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testCompressBzip2() {
        Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
            Js5Compression.compress(expected.slice(), Js5CompressionType.BZIP2).use { compressed ->
                Js5Compression.uncompress(compressed).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testUncompressBzip2() {
        Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
            read("bzip2.dat").use { input ->
                Js5Compression.uncompress(input).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testCompressLzma() {
        Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
            Js5Compression.compress(expected.slice(), Js5CompressionType.LZMA).use { compressed ->
                Js5Compression.uncompress(compressed).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testUncompressLzma() {
        Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
            read("lzma.dat").use { input ->
                Js5Compression.uncompress(input).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testCompressNoneEncrypted() {
        read("none-encrypted.dat").use { expected ->
            Unpooled.wrappedBuffer("OpenRS2".repeat(3).toByteArray()).use { input ->
                Js5Compression.compress(input, Js5CompressionType.NONE, KEY).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testUncompressNoneEncrypted() {
        Unpooled.wrappedBuffer("OpenRS2".repeat(3).toByteArray()).use { expected ->
            read("none-encrypted.dat").use { input ->
                Js5Compression.uncompress(input, KEY).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testCompressGzipEncrypted() {
        Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
            Js5Compression.compress(expected.slice(), Js5CompressionType.GZIP, KEY).use { compressed ->
                Js5Compression.uncompress(compressed, KEY).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testUncompressGzipEncrypted() {
        Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
            read("gzip-encrypted.dat").use { input ->
                Js5Compression.uncompress(input, KEY).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testCompressBzip2Encrypted() {
        Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
            Js5Compression.compress(expected.slice(), Js5CompressionType.BZIP2, KEY).use { compressed ->
                Js5Compression.uncompress(compressed, KEY).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testUncompressBzip2Encrypted() {
        Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
            read("bzip2-encrypted.dat").use { input ->
                Js5Compression.uncompress(input, KEY).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testCompressLzmaEncrypted() {
        Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
            Js5Compression.compress(expected.slice(), Js5CompressionType.LZMA, KEY).use { compressed ->
                Js5Compression.uncompress(compressed, KEY).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testUncompressLzmaEncrypted() {
        Unpooled.wrappedBuffer("OpenRS2".toByteArray()).use { expected ->
            read("lzma-encrypted.dat").use { input ->
                Js5Compression.uncompress(input, KEY).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testCompressBest() {
        Unpooled.wrappedBuffer("OpenRS2".repeat(100).toByteArray()).use { expected ->
            val noneLen = Js5Compression.compress(expected.slice(), Js5CompressionType.NONE).use { compressed ->
                compressed.readableBytes()
            }

            Js5Compression.compressBest(expected.slice()).use { compressed ->
                assertNotEquals(Js5CompressionType.NONE.ordinal, compressed.getUnsignedByte(0).toInt())
                assert(compressed.readableBytes() < noneLen)

                Js5Compression.uncompress(compressed).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testCompressBestEncrypted() {
        Unpooled.wrappedBuffer("OpenRS2".repeat(100).toByteArray()).use { expected ->
            val noneLen = Js5Compression.compress(expected.slice(), Js5CompressionType.NONE).use { compressed ->
                compressed.readableBytes()
            }

            Js5Compression.compressBest(expected.slice(), key = KEY).use { compressed ->
                assertNotEquals(Js5CompressionType.NONE.ordinal, compressed.getUnsignedByte(0).toInt())
                assert(compressed.readableBytes() < noneLen)

                Js5Compression.uncompress(compressed, KEY).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    private fun read(name: String): ByteBuf {
        Js5CompressionTest::class.java.getResourceAsStream("compression/$name").use { input ->
            return Unpooled.wrappedBuffer(input.readAllBytes())
        }
    }
}
