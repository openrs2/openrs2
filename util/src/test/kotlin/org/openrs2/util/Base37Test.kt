package org.openrs2.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Base37Test {
    @Test
    fun testEncodeBounds() {
        assertEquals(0, Base37.encode(""))
        assertEquals(1, Base37.encode("a"))
        assertEquals(1, Base37.encode("A"))
        assertEquals(6582952005840035279, Base37.encode("999999999998"))
        assertEquals(6582952005840035280, Base37.encode("999999999999"))

        assertFailsWith<IllegalArgumentException> {
            Base37.encode("aaaaaaaaaaaaa")
        }
    }

    @Test
    fun testEncodeBoundsWhitespace() {
        assertEquals(6582952005840035280, Base37.encode("999999999999 "))
        assertEquals(6582952005840035280, Base37.encode(" 999999999999"))
        assertEquals(6582952005840035280, Base37.encode(" 999999999999 "))

        assertFailsWith<IllegalArgumentException> {
            Base37.encode("aaaaaaaaaaaaa ")
        }

        assertFailsWith<IllegalArgumentException> {
            Base37.encode(" aaaaaaaaaaaaa")
        }

        assertFailsWith<IllegalArgumentException> {
            Base37.encode(" aaaaaaaaaaaaa ")
        }
    }

    @Test
    fun testDecodeLowerCaseBounds() {
        assertFailsWith<IllegalArgumentException> {
            Base37.decodeLowerCase(-1)
        }

        assertEquals("", Base37.decodeLowerCase(0))
        assertEquals("a", Base37.decodeLowerCase(1))
        assertEquals("999999999998", Base37.decodeLowerCase(6582952005840035279))
        assertEquals("999999999999", Base37.decodeLowerCase(6582952005840035280))

        assertFailsWith<IllegalArgumentException> {
            Base37.decodeLowerCase(6582952005840035281)
        }
    }

    @Test
    fun testDecodeTitleCaseBounds() {
        assertFailsWith<IllegalArgumentException> {
            Base37.decodeTitleCase(-1)
        }

        assertEquals("", Base37.decodeTitleCase(0))
        assertEquals("A", Base37.decodeTitleCase(1))
        assertEquals("999999999998", Base37.decodeTitleCase(6582952005840035279))
        assertEquals("999999999999", Base37.decodeTitleCase(6582952005840035280))

        assertFailsWith<IllegalArgumentException> {
            Base37.decodeTitleCase(6582952005840035281)
        }
    }

    @Test
    fun testEncodeInvalidChar() {
        assertFailsWith<IllegalArgumentException> {
            Base37.encode("!")
        }
    }

    @Test
    fun testEncodeWhitespace() {
        assertEquals(1465402762952, Base37.encode("Open Rs2"))
        assertEquals(1465402762952, Base37.encode("open_rs2"))
        assertEquals(1465402762952, Base37.encode(" Open Rs2 "))
        assertEquals(1465402762952, Base37.encode("_Open Rs2_"))
    }

    @Test
    fun testDecodeLowerCaseWhitespace() {
        assertEquals("open_rs2", Base37.decodeLowerCase(1465402762952))
    }

    @Test
    fun testDecodeTitleCaseWhitespace() {
        assertEquals("Open Rs2", Base37.decodeTitleCase(1465402762952))
    }

    @Test
    fun testDecodeLowerCaseTrailingWhitespace() {
        assertFailsWith<IllegalArgumentException> {
            Base37.decodeLowerCase(54219902229224)
        }
    }

    @Test
    fun testDecodeTitleCaseTrailingWhitespace() {
        assertFailsWith<IllegalArgumentException> {
            Base37.decodeTitleCase(54219902229224)
        }
    }

    @Test
    fun testToLowerCase() {
        assertEquals("open_rs2", Base37.toLowerCase(" OpEn rS2_"))
    }

    @Test
    fun testToTitleCase() {
        assertEquals("Open Rs2", Base37.toTitleCase(" OpEn rS2_"))
    }
}
