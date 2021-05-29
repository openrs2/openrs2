package org.openrs2.util.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UniqueQueueTest {
    @Test
    fun testAddPoll() {
        val queue = UniqueQueue<String>()
        assertTrue(queue.add("a"))
        assertTrue(queue.add("b"))
        assertFalse(queue.add("a"))

        assertEquals("a", queue.poll())
        assertEquals("b", queue.poll())
        assertNull(queue.poll())
    }

    @Test
    fun testAddAll() {
        val queue = UniqueQueue<String>()
        queue.addAll(listOf("a", "b", "a"))

        assertEquals("a", queue.poll())
        assertEquals("b", queue.poll())
        assertNull(queue.poll())
    }

    @Test
    fun testClear() {
        val queue = UniqueQueue<String>()
        assertTrue(queue.add("a"))

        queue.clear()

        assertNull(queue.poll())
    }
}
