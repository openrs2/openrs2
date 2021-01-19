package org.openrs2.cache

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.openrs2.buffer.use
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

object Js5MasterIndexTest {
    private val ROOT = Path.of(FlatFileStoreTest::class.java.getResource("master-index").toURI())
    private val encoded = byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 2, 0, 0, 0, 5, 0, 0, 0, 4)
    private val decoded = Js5MasterIndex(
        mutableListOf(
            Js5MasterIndex.Entry(0, 1),
            Js5MasterIndex.Entry(2, 3),
            Js5MasterIndex.Entry(4, 5)
        )
    )

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
    fun testRead() {
        Unpooled.wrappedBuffer(encoded).use { buf ->
            val index = Js5MasterIndex.read(buf)
            assertEquals(decoded, index)
        }
    }

    @Test
    fun testWrite() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            decoded.write(actual)

            Unpooled.wrappedBuffer(encoded).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }
}
