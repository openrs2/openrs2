package org.openrs2.util.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IterableUtilsTest {
    @Test
    fun testRemoveFirst() {
        val list = mutableListOf(1, 2, 3)
        assertEquals(1, list.removeFirst())
        assertEquals(listOf(2, 3), list)

        assertEquals(2, list.removeFirst())
        assertEquals(listOf(3), list)

        assertEquals(3, list.removeFirst())
        assertEquals(emptyList<Int>(), list)

        assertFailsWith<NoSuchElementException> {
            list.removeFirst()
        }
    }

    @Test
    fun testRemoveFirstOrNull() {
        val list = mutableListOf(1, 2, 3)
        assertEquals(1, list.removeFirstOrNull())
        assertEquals(listOf(2, 3), list)

        assertEquals(2, list.removeFirstOrNull())
        assertEquals(listOf(3), list)

        assertEquals(3, list.removeFirstOrNull())
        assertEquals(emptyList<Int>(), list)

        assertNull(list.removeFirstOrNull())
    }

    @Test
    fun testRemoveFirstMatching() {
        val list = mutableListOf(1, 2, 2, 3)
        assertTrue(list.removeFirst { it == 2 })
        assertEquals(listOf(1, 2, 3), list)

        assertTrue(list.removeFirst { it == 2 })
        assertEquals(listOf(1, 3), list)

        assertFalse(list.removeFirst { it == 2 })
        assertEquals(listOf(1, 3), list)
    }
}
