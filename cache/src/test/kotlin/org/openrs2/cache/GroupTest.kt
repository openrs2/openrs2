package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMaps
import org.openrs2.buffer.use
import org.openrs2.buffer.wrappedBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GroupTest {
    @Test
    fun testPackEmpty() {
        assertFailsWith<IllegalArgumentException> {
            Group.pack(Int2ObjectSortedMaps.emptyMap()).release()
        }
    }

    @Test
    fun testUnpackEmpty() {
        assertFailsWith<IllegalArgumentException> {
            val files = Group.unpack(Unpooled.EMPTY_BUFFER, zeroFiles)
            files.values.forEach(ByteBuf::release)
        }

        assertFailsWith<IllegalArgumentException> {
            val files = Group.unpack(Unpooled.EMPTY_BUFFER, multipleFiles)
            files.values.forEach(ByteBuf::release)
        }
    }

    @Test
    fun testPackSingle() {
        wrappedBuffer(0, 1, 2, 3).use { buf ->
            assertEquals(buf, Group.pack(Int2ObjectSortedMaps.singleton(1, buf)))
        }
    }

    @Test
    fun testUnpackSingle() {
        wrappedBuffer(0, 1, 2, 3).use { buf ->
            val expected = Int2ObjectSortedMaps.singleton(1, buf)
            val actual = Group.unpack(buf.slice(), oneFile)
            try {
                assertEquals(expected, actual)
            } finally {
                actual.values.forEach(ByteBuf::release)
            }
        }
    }

    @Test
    fun testPackZeroStripes() {
        val files = Int2ObjectAVLTreeMap<ByteBuf>()
        files[0] = Unpooled.EMPTY_BUFFER
        files[1] = Unpooled.EMPTY_BUFFER
        files[3] = Unpooled.EMPTY_BUFFER

        wrappedBuffer(0).use { expected ->
            Group.pack(files).use { actual ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testUnpackZeroStripes() {
        val expected = Int2ObjectAVLTreeMap<ByteBuf>()
        expected[0] = Unpooled.EMPTY_BUFFER
        expected[1] = Unpooled.EMPTY_BUFFER
        expected[3] = Unpooled.EMPTY_BUFFER

        wrappedBuffer(0).use { buf ->
            val actual = Group.unpack(buf, multipleFiles)
            try {
                assertEquals(expected, actual)
            } finally {
                actual.values.forEach(ByteBuf::release)
            }
        }
    }

    @Test
    fun testPackOneStripe() {
        val files = Int2ObjectAVLTreeMap<ByteBuf>()
        try {
            files[0] = wrappedBuffer(0, 1, 2)
            files[1] = wrappedBuffer(3, 4, 5, 6, 7)
            files[3] = wrappedBuffer(8, 9)

            wrappedBuffer(
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                0, 0, 0, 3,
                0, 0, 0, 2,
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFD.toByte(),
                1
            ).use { expected ->
                Group.pack(files).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        } finally {
            files.values.forEach(ByteBuf::release)
        }
    }

    @Test
    fun testUnpackOneStripe() {
        val expected = Int2ObjectAVLTreeMap<ByteBuf>()
        try {
            expected[0] = wrappedBuffer(0, 1, 2)
            expected[1] = wrappedBuffer(3, 4, 5, 6, 7)
            expected[3] = wrappedBuffer(8, 9)

            wrappedBuffer(
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                0, 0, 0, 3,
                0, 0, 0, 2,
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFD.toByte(),
                1
            ).use { buf ->
                val actual = Group.unpack(buf, multipleFiles)
                try {
                    assertEquals(expected, actual)
                } finally {
                    actual.values.forEach(ByteBuf::release)
                }
            }
        } finally {
            expected.values.forEach(ByteBuf::release)
        }
    }

    @Test
    fun testUnpackMultipleStripes() {
        val expected = Int2ObjectAVLTreeMap<ByteBuf>()
        try {
            expected[0] = wrappedBuffer(0, 1, 2)
            expected[1] = wrappedBuffer(3, 4, 5, 6, 7)
            expected[3] = wrappedBuffer(8, 9)

            wrappedBuffer(
                0, 1,
                3, 4,
                8, 9,
                2,
                5, 6, 7,
                0, 0, 0, 2,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 1,
                0, 0, 0, 2,
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFD.toByte(),
                2
            ).use { buf ->
                val actual = Group.unpack(buf, multipleFiles)
                try {
                    assertEquals(expected, actual)
                } finally {
                    actual.values.forEach(ByteBuf::release)
                }
            }
        } finally {
            expected.values.forEach(ByteBuf::release)
        }
    }

    private companion object {
        private val index = Js5Index(Js5Protocol.ORIGINAL)
        private val zeroFiles = index.createOrGet(0)
        private val oneFile = index.createOrGet(1).apply {
            createOrGet(1)
        }
        private val multipleFiles = index.createOrGet(2).apply {
            createOrGet(0)
            createOrGet(1)
            createOrGet(3)
        }
    }
}
