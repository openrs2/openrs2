package org.openrs2.cache

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

object Js5MasterIndexTest {
    private val ROOT = Paths.get(FlatFileStoreTest::class.java.getResource("master-index").toURI())

    @Test
    fun testCreate() {
        val index = Store.open(ROOT).use { store ->
            Js5MasterIndex.create(store)
        }

        assertEquals(Js5MasterIndex(mutableListOf(
            Js5MasterIndex.Entry(0, 379203374),
            Js5MasterIndex.Entry(0x12345678, -717247318),
            Js5MasterIndex.Entry(0, 0),
            Js5MasterIndex.Entry(0x9ABCDEF0.toInt(), 895417101),
            Js5MasterIndex.Entry(0, 0),
            Js5MasterIndex.Entry(0, 0),
            Js5MasterIndex.Entry(0xAA55AA55.toInt(), -627983571)
        )), index)
    }
}
