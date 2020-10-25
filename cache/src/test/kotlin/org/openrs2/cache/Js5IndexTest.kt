package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.assertThrows
import org.openrs2.buffer.use
import org.openrs2.buffer.wrappedBuffer
import org.openrs2.util.krHashCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

object Js5IndexTest {
    private val emptyIndex = Js5Index(Js5Protocol.ORIGINAL)

    private val versionedIndex = Js5Index(Js5Protocol.VERSIONED, version = 0x12345678)

    private val noFlagsIndex = Js5Index(Js5Protocol.ORIGINAL).apply {
        val group0 = createOrGet(0)
        group0.checksum = 0x01234567
        group0.version = 0
        group0.createOrGet(0)

        val group1 = createOrGet(1)
        group1.checksum = 0x89ABCDEF.toInt()
        group1.version = 10

        val group2 = createOrGet(3)
        group2.checksum = 0xAAAA5555.toInt()
        group2.version = 20
        group2.createOrGet(1)
        group2.createOrGet(3)
    }

    private val namedIndex = Js5Index(Js5Protocol.ORIGINAL, hasNames = true).apply {
        val group0 = createOrGet("hello")
        group0.checksum = 0x01234567
        group0.version = 0x89ABCDEF.toInt()
        group0.createOrGet("world")
    }

    private val smartIndex = Js5Index(Js5Protocol.SMART).apply {
        val group0 = createOrGet(0)
        group0.checksum = 0x01234567
        group0.version = 0x89ABCDEF.toInt()
        group0.createOrGet(0)
        group0.createOrGet(100000)

        val group1 = createOrGet(100000)
        group1.checksum = 0xAAAA5555.toInt()
        group1.version = 0x5555AAAA
    }

    private val digestIndex = Js5Index(Js5Protocol.ORIGINAL, hasDigests = true).apply {
        val group = createOrGet(0)
        group.checksum = 0x01234567
        group.version = 0x89ABCDEF.toInt()
        group.digest = ByteBufUtil.decodeHexDump(
            "19FA61D75522A4669B44E39C1D2E1726C530232130D407F89AFEE0964997F7A7" +
                "3E83BE698B288FEBCF88E3E03C4F0757EA8964E59B63D93708B138CC42A66EB3"
        )
    }

    private val nullDigestIndex = Js5Index(Js5Protocol.ORIGINAL, hasDigests = true).apply {
        val group = createOrGet(0)
        group.checksum = 0x01234567
        group.version = 0x89ABCDEF.toInt()
        group.digest = null
    }

    private val lengthsIndex = Js5Index(Js5Protocol.ORIGINAL, hasLengths = true).apply {
        val group = createOrGet(0)
        group.checksum = 0x01234567
        group.version = 0x89ABCDEF.toInt()
        group.length = 1000
        group.uncompressedLength = 2000
    }

    private val uncompressedChecksumIndex = Js5Index(Js5Protocol.ORIGINAL, hasUncompressedChecksums = true).apply {
        val group = createOrGet(0)
        group.checksum = 0x01234567
        group.version = 0x89ABCDEF.toInt()
        group.uncompressedChecksum = 0xAAAA5555.toInt()
    }

    private val allFlagsIndex = Js5Index(
        Js5Protocol.ORIGINAL,
        hasNames = true,
        hasDigests = true,
        hasLengths = true,
        hasUncompressedChecksums = true
    ).apply {
        val group = createOrGet("hello")
        group.checksum = 0x01234567
        group.version = 0x89ABCDEF.toInt()
        group.digest = ByteBufUtil.decodeHexDump(
            "19FA61D75522A4669B44E39C1D2E1726C530232130D407F89AFEE0964997F7A7" +
                "3E83BE698B288FEBCF88E3E03C4F0757EA8964E59B63D93708B138CC42A66EB3"
        )
        group.length = 1000
        group.uncompressedLength = 2000
        group.uncompressedChecksum = 0xAAAA5555.toInt()
        group.createOrGet("world")
    }

    @Test
    fun testReadEmpty() {
        read("empty.dat").use { buf ->
            val index = Js5Index.read(buf)
            assertFalse(buf.isReadable)
            assertEquals(emptyIndex, index)
        }
    }

    @Test
    fun testWriteEmpty() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            emptyIndex.write(actual)

            read("empty.dat").use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testReadUnsupportedProtocol() {
        wrappedBuffer(4).use { buf ->
            assertThrows<IllegalArgumentException> {
                Js5Index.read(buf)
            }
        }

        wrappedBuffer(8).use { buf ->
            assertThrows<IllegalArgumentException> {
                Js5Index.read(buf)
            }
        }
    }

    @Test
    fun testReadVersioned() {
        read("versioned.dat").use { buf ->
            val index = Js5Index.read(buf)
            assertFalse(buf.isReadable)
            assertEquals(versionedIndex, index)
        }
    }

    @Test
    fun testWriteVersioned() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            versionedIndex.write(actual)

            read("versioned.dat").use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testReadNoFlags() {
        read("no-flags.dat").use { buf ->
            val index = Js5Index.read(buf)
            assertFalse(buf.isReadable)
            assertEquals(noFlagsIndex, index)
        }
    }

    @Test
    fun testWriteNoFlags() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            noFlagsIndex.write(actual)

            read("no-flags.dat").use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testReadNamed() {
        read("named.dat").use { buf ->
            val index = Js5Index.read(buf)
            assertFalse(buf.isReadable)
            assertEquals(namedIndex, index)
        }
    }

    @Test
    fun testWriteNamed() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            namedIndex.write(actual)

            read("named.dat").use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testReadSmart() {
        read("smart.dat").use { buf ->
            val index = Js5Index.read(buf)
            assertFalse(buf.isReadable)
            assertEquals(smartIndex, index)
        }
    }

    @Test
    fun testWriteSmart() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            smartIndex.write(actual)

            read("smart.dat").use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testWriteNonSmartOutOfRange() {
        val index = Js5Index(Js5Protocol.ORIGINAL)
        index.createOrGet(65536)

        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            assertThrows<IllegalStateException> {
                index.write(buf)
            }
        }
    }

    @Test
    fun testReadDigest() {
        read("digest.dat").use { buf ->
            val index = Js5Index.read(buf)
            assertFalse(buf.isReadable)
            assertEquals(digestIndex, index)
        }
    }

    @Test
    fun testWriteDigest() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            digestIndex.write(actual)

            read("digest.dat").use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testWriteNullDigest() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            nullDigestIndex.write(actual)

            read("null-digest.dat").use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testReadLengths() {
        read("lengths.dat").use { buf ->
            val index = Js5Index.read(buf)
            assertFalse(buf.isReadable)
            assertEquals(lengthsIndex, index)
        }
    }

    @Test
    fun testWriteLengths() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            lengthsIndex.write(actual)

            read("lengths.dat").use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testReadUncompressedChecksum() {
        read("uncompressed-checksum.dat").use { buf ->
            val index = Js5Index.read(buf)
            assertFalse(buf.isReadable)
            assertEquals(uncompressedChecksumIndex, index)
        }
    }

    @Test
    fun testWriteUncompressedChecksum() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            uncompressedChecksumIndex.write(actual)

            read("uncompressed-checksum.dat").use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testReadAllFlags() {
        read("all-flags.dat").use { buf ->
            val index = Js5Index.read(buf)
            assertFalse(buf.isReadable)
            assertEquals(allFlagsIndex, index)
        }
    }

    @Test
    fun testWriteAllFlags() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            allFlagsIndex.write(actual)

            read("all-flags.dat").use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testRenameGroup() {
        val index = Js5Index(Js5Protocol.ORIGINAL)

        val group = index.createOrGet(0)
        assertEquals(-1, group.nameHash)
        assertNull(index["hello"])

        group.setName("hello")
        assertEquals("hello".krHashCode(), group.nameHash)
        assertEquals(group, index["hello"])

        group.clearName()
        assertEquals(-1, group.nameHash)
        assertNull(index["hello"])
    }

    @Test
    fun testRenameFile() {
        val index = Js5Index(Js5Protocol.ORIGINAL)
        val group = index.createOrGet(0)

        val file = group.createOrGet(0)
        assertEquals(-1, file.nameHash)
        assertNull(group["hello"])

        file.setName("hello")
        assertEquals("hello".krHashCode(), file.nameHash)
        assertEquals(file, group["hello"])

        file.clearName()
        assertEquals(-1, file.nameHash)
        assertNull(group["hello"])
    }

    @Test
    fun testRemoveGroup() {
        val index = Js5Index(Js5Protocol.ORIGINAL)

        val group = index.createOrGet(0)
        assertEquals(1, index.size)
        assertEquals(1, index.capacity)
        assertTrue(index.contains(0))
        assertEquals(group, index[0])

        group.remove()
        assertEquals(0, index.size)
        assertEquals(0, index.capacity)
        assertFalse(index.contains(0))
        assertNull(index[0])

        group.remove()
        assertEquals(0, index.size)
        assertEquals(0, index.capacity)
        assertFalse(index.contains(0))
        assertNull(index[0])
    }

    @Test
    fun testRemoveFile() {
        val index = Js5Index(Js5Protocol.ORIGINAL)
        val group = index.createOrGet(0)

        val file = group.createOrGet(0)
        assertEquals(1, group.size)
        assertEquals(1, group.capacity)
        assertTrue(group.contains(0))
        assertEquals(file, group[0])

        file.remove()
        assertEquals(0, group.size)
        assertEquals(0, group.capacity)
        assertFalse(group.contains(0))
        assertNull(group[0])

        file.remove()
        assertEquals(0, group.size)
        assertEquals(0, group.capacity)
        assertFalse(group.contains(0))
        assertNull(group[0])
    }

    private fun read(name: String): ByteBuf {
        Js5IndexTest::class.java.getResourceAsStream("index/$name").use { input ->
            return Unpooled.wrappedBuffer(input.readAllBytes())
        }
    }
}
