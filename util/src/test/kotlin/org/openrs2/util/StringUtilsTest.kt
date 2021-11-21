package org.openrs2.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringUtilsTest {
    @Test
    fun testIndefiniteArticle() {
        assertEquals("a", "book".indefiniteArticle())
        assertEquals("an", "aeroplane".indefiniteArticle())
        assertFailsWith(IllegalArgumentException::class) {
            "".indefiniteArticle()
        }
    }

    @Test
    fun testKrHashCode() {
        assertEquals(0, "".krHashCode())
        assertEquals(99162322, "hello".krHashCode())
        assertEquals(92340183, "h€llo".krHashCode())
    }

    @Test
    fun testJagHashCode() {
        assertEquals(0, "".jagHashCode())
        assertEquals(1012849752, "hello".jagHashCode())
    }

    @Test
    fun testCapitalize() {
        assertEquals("Hello", "hello".capitalize())
        assertEquals("Hello", "Hello".capitalize())
        assertEquals("HELLO", "HELLO".capitalize())
    }
}
