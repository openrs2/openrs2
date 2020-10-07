package org.openrs2.util.charset

import org.junit.jupiter.api.Assertions.assertArrayEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object ModifiedUtf8CharsetTest {
    @Test
    fun testEncode() {
        assertArrayEquals(byteArrayOf(0xC0.toByte(), 0x80.toByte()), "\u0000".toByteArray(ModifiedUtf8Charset))
        assertArrayEquals(byteArrayOf(0x41), "A".toByteArray(ModifiedUtf8Charset))
        assertArrayEquals(byteArrayOf(0xC2.toByte(), 0xA9.toByte()), "©".toByteArray(ModifiedUtf8Charset))
        assertArrayEquals(
            byteArrayOf(0xE2.toByte(), 0x82.toByte(), 0xAC.toByte()),
            "€".toByteArray(ModifiedUtf8Charset)
        )
    }

    @Test
    fun testDecode() {
        assertEquals("\u0000", String(byteArrayOf(0xC0.toByte(), 0x80.toByte()), ModifiedUtf8Charset))
        assertEquals("A", String(byteArrayOf(0x41), ModifiedUtf8Charset))
        assertEquals("©", String(byteArrayOf(0xC2.toByte(), 0xA9.toByte()), ModifiedUtf8Charset))
        assertEquals(
            "€",
            String(byteArrayOf(0xE2.toByte(), 0x82.toByte(), 0xAC.toByte()), ModifiedUtf8Charset)
        )

        assertEquals("\uFFFD", String(byteArrayOf(0), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0x80.toByte()), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0xC0.toByte()), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0xC0.toByte(), 0.toByte()), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0xE0.toByte()), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0xE0.toByte(), 0.toByte()), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0xE0.toByte(), 0x80.toByte()), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0xE0.toByte(), 0x80.toByte(), 0.toByte()), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0xE0.toByte(), 0, 0x80.toByte()), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0xF0.toByte()), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0xF8.toByte()), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0xFC.toByte()), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0xFC.toByte()), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0xFE.toByte()), ModifiedUtf8Charset))
        assertEquals("\uFFFD", String(byteArrayOf(0xFF.toByte()), ModifiedUtf8Charset))
    }

    @Test
    fun testContains() {
        assertTrue(ModifiedUtf8Charset.contains(ModifiedUtf8Charset))
        assertTrue(ModifiedUtf8Charset.contains(Cp1252Charset))
        assertTrue(ModifiedUtf8Charset.contains(Charsets.US_ASCII))
        assertTrue(ModifiedUtf8Charset.contains(Charsets.ISO_8859_1))
        assertTrue(ModifiedUtf8Charset.contains(Charsets.UTF_8))
        assertTrue(ModifiedUtf8Charset.contains(Charsets.UTF_16))
        assertTrue(ModifiedUtf8Charset.contains(Charsets.UTF_16BE))
        assertTrue(ModifiedUtf8Charset.contains(Charsets.UTF_16LE))
    }
}
