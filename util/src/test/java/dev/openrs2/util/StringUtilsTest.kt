package dev.openrs2.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object StringUtilsTest {
    @Test
    fun testIndefiniteArticle() {
        assertEquals("a", "book".indefiniteArticle())
        assertEquals("an", "aeroplane".indefiniteArticle())
        assertFailsWith(IllegalArgumentException::class) {
            "".indefiniteArticle()
        }
    }
}
