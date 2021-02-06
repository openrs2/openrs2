package org.openrs2.cache

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.assertThrows
import org.openrs2.buffer.use
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

object Js5MasterIndexTest {
    private val ROOT = Path.of(FlatFileStoreTest::class.java.getResource("master-index").toURI())

    private val encodedOriginal = byteArrayOf(0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 5)
    private val decodedOriginal = Js5MasterIndex(
        mutableListOf(
            Js5MasterIndex.Entry(0, 1),
            Js5MasterIndex.Entry(0, 3),
            Js5MasterIndex.Entry(0, 5)
        )
    )

    private val encodedVersioned = byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 2, 0, 0, 0, 5, 0, 0, 0, 4)
    private val decodedVersioned = Js5MasterIndex(
        mutableListOf(
            Js5MasterIndex.Entry(0, 1),
            Js5MasterIndex.Entry(2, 3),
            Js5MasterIndex.Entry(4, 5)
        )
    )

    @Test
    fun testMinimumFormat() {
        assertEquals(MasterIndexFormat.ORIGINAL, decodedOriginal.minimumFormat)
        assertEquals(MasterIndexFormat.VERSIONED, decodedVersioned.minimumFormat)
    }

    @Test
    fun testCreate() {
        val index = Store.open(ROOT).use { store ->
            Js5MasterIndex.create(store)
        }

        assertEquals(
            Js5MasterIndex(
                mutableListOf(
                    Js5MasterIndex.Entry(0, 609698396),
                    Js5MasterIndex.Entry(0x12345678, 78747481),
                    Js5MasterIndex.Entry(0, 0),
                    Js5MasterIndex.Entry(0x9ABCDEF0.toInt(), -456081154),
                    Js5MasterIndex.Entry(0, 0),
                    Js5MasterIndex.Entry(0, 0),
                    Js5MasterIndex.Entry(0xAA55AA55.toInt(), 186613982)
                )
            ), index
        )
    }

    @Test
    fun testReadOriginal() {
        Unpooled.wrappedBuffer(encodedOriginal).use { buf ->
            val index = Js5MasterIndex.read(buf, MasterIndexFormat.ORIGINAL)
            assertEquals(decodedOriginal, index)
        }

        assertThrows<IllegalArgumentException> {
            Unpooled.wrappedBuffer(byteArrayOf(0)).use { buf ->
                Js5MasterIndex.read(buf, MasterIndexFormat.ORIGINAL)
            }
        }
    }

    @Test
    fun testWriteOriginal() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            decodedOriginal.write(actual, MasterIndexFormat.ORIGINAL)

            Unpooled.wrappedBuffer(encodedOriginal).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testReadVersioned() {
        Unpooled.wrappedBuffer(encodedVersioned).use { buf ->
            val index = Js5MasterIndex.read(buf, MasterIndexFormat.VERSIONED)
            assertEquals(decodedVersioned, index)
        }

        assertThrows<IllegalArgumentException> {
            Unpooled.wrappedBuffer(byteArrayOf(0, 0, 0, 0)).use { buf ->
                Js5MasterIndex.read(buf, MasterIndexFormat.VERSIONED)
            }
        }
    }

    @Test
    fun testWriteVersioned() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            decodedVersioned.write(actual, MasterIndexFormat.VERSIONED)

            Unpooled.wrappedBuffer(encodedVersioned).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }
}
