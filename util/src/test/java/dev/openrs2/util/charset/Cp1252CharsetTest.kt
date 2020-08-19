package dev.openrs2.util.charset

import org.junit.jupiter.api.Assertions.assertArrayEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object Cp1252CharsetTest {
    @Test
    fun testEncodeChar() {
        // edge cases
        assertEquals(Cp1252Charset.encode('\u0000'), '?'.toByte())
        assertEquals(Cp1252Charset.encode('\u0001'), 1.toByte())
        assertEquals(Cp1252Charset.encode('\u007F'), 127.toByte())
        assertEquals(Cp1252Charset.encode('€'), 128.toByte())
        assertEquals(Cp1252Charset.encode('Ÿ'), 159.toByte())
        assertEquals(Cp1252Charset.encode('\u00A0'), 160.toByte())
        assertEquals(Cp1252Charset.encode('ÿ'), 255.toByte())
        assertEquals(Cp1252Charset.encode('\u0100'), '?'.toByte())

        // 7-bit ASCII char
        assertEquals(Cp1252Charset.encode('A'), 65.toByte())

        // CP-1252 char
        assertEquals(Cp1252Charset.encode('Š'), 138.toByte())

        // extended ASCII char
        assertEquals(Cp1252Charset.encode('Ö'), 214.toByte())
    }

    @Test
    fun testDecodeChar() {
        // edge cases
        assertEquals('\uFFFD', Cp1252Charset.decode(0.toByte()))
        assertEquals('\u0001', Cp1252Charset.decode(1.toByte()))
        assertEquals('\u007F', Cp1252Charset.decode(127.toByte()))
        assertEquals('€', Cp1252Charset.decode(128.toByte()))
        assertEquals('Ÿ', Cp1252Charset.decode(159.toByte()))
        assertEquals('\u00A0', Cp1252Charset.decode(160.toByte()))
        assertEquals('ÿ', Cp1252Charset.decode(255.toByte()))

        // 7-bit ASCII char
        assertEquals('A', Cp1252Charset.decode(65.toByte()))

        // CP-1252 char
        assertEquals('Š', Cp1252Charset.decode(138.toByte()))

        // extended ASCII char
        assertEquals('Ö', Cp1252Charset.decode(214.toByte()))

        // invalid chars in the CP-1252 code page
        assertEquals('\uFFFD', Cp1252Charset.decode(129.toByte()))
        assertEquals('\uFFFD', Cp1252Charset.decode(141.toByte()))
        assertEquals('\uFFFD', Cp1252Charset.decode(143.toByte()))
        assertEquals('\uFFFD', Cp1252Charset.decode(144.toByte()))
        assertEquals('\uFFFD', Cp1252Charset.decode(157.toByte()))
    }

    @Test
    fun testEncode() {
        assertArrayEquals(
            byteArrayOf(
                '?'.toByte(),
                1.toByte(),
                127.toByte(),
                128.toByte(),
                159.toByte(),
                160.toByte(),
                255.toByte(),
                '?'.toByte(),
                65.toByte(),
                138.toByte(),
                214.toByte()
            ), "\u0000\u0001\u007F€Ÿ\u00A0ÿ\u0100AŠÖ".toByteArray(Cp1252Charset)
        )
    }

    @Test
    fun testDecode() {
        assertEquals(
            "\uFFFD\u0001\u007F€Ÿ\u00A0ÿAŠÖ\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD", String(
                byteArrayOf(
                    0.toByte(),
                    1.toByte(),
                    127.toByte(),
                    128.toByte(),
                    159.toByte(),
                    160.toByte(),
                    255.toByte(),
                    65.toByte(),
                    138.toByte(),
                    214.toByte(),
                    129.toByte(),
                    141.toByte(),
                    143.toByte(),
                    144.toByte(),
                    157.toByte()
                ), Cp1252Charset
            )
        )
    }

    @Test
    fun testContains() {
        assertTrue(Cp1252Charset.contains(Cp1252Charset))
        assertTrue(Cp1252Charset.contains(Charsets.US_ASCII))
        assertFalse(Cp1252Charset.contains(Charsets.ISO_8859_1))
        assertFalse(Cp1252Charset.contains(Charsets.UTF_8))
        assertFalse(Cp1252Charset.contains(Charsets.UTF_16))
        assertFalse(Cp1252Charset.contains(Charsets.UTF_16BE))
        assertFalse(Cp1252Charset.contains(Charsets.UTF_16LE))
    }
}
