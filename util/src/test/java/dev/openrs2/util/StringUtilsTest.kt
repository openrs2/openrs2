package dev.openrs2.util

import dev.openrs2.util.StringUtils.capitalize
import dev.openrs2.util.StringUtils.indefiniteArticle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object StringUtilsTest {
    @Test
    fun testIndefiniteArticle() {
        assertEquals("a", indefiniteArticle("book"))
        assertEquals("an", indefiniteArticle("aeroplane"))
        assertFailsWith(IllegalArgumentException::class) {
            indefiniteArticle("")
        }
    }

    @Test
    fun testCapitalize() {
        assertEquals("Hello", capitalize("hello"))
        assertEquals("", capitalize(""))
    }
}
