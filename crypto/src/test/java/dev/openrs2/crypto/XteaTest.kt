package dev.openrs2.crypto

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import kotlin.test.Test
import kotlin.test.assertEquals

object XteaTest {
    private class TestVector(key: String, plaintext: String, ciphertext: String) {
        val key = IntArray(4) {
            Integer.parseUnsignedInt(key, it * 8, it * 8 + 8, 16)
        }
        val plaintext: ByteArray = ByteBufUtil.decodeHexDump(plaintext)
        val ciphertext: ByteArray = ByteBufUtil.decodeHexDump(ciphertext)
    }

    private val TEST_VECTORS = listOf(
        // empty
        TestVector("00000000000000000000000000000000", "", ""),

        // standard single block test vectors
        TestVector("000102030405060708090a0b0c0d0e0f", "4142434445464748", "497df3d072612cb5"),
        TestVector("000102030405060708090a0b0c0d0e0f", "4141414141414141", "e78f2d13744341d8"),
        TestVector("000102030405060708090a0b0c0d0e0f", "5a5b6e278948d77f", "4141414141414141"),
        TestVector("00000000000000000000000000000000", "4142434445464748", "a0390589f8b8efa5"),
        TestVector("00000000000000000000000000000000", "4141414141414141", "ed23375a821a8c2d"),
        TestVector("00000000000000000000000000000000", "70e1225d6e4e7655", "4141414141414141"),

        // two blocks
        TestVector("00000000000000000000000000000000", "70e1225d6e4e76554141414141414141",
            "4141414141414141ed23375a821a8c2d"),

        // not a multiple of the block size
        TestVector("00000000000000000000000000000000", "01", "01"),
        TestVector("00000000000000000000000000000000", "01020304050607", "01020304050607"),
        TestVector("00000000000000000000000000000000", "70e1225d6e4e765501", "414141414141414101"),
        TestVector("00000000000000000000000000000000", "70e1225d6e4e765501020304050607",
            "414141414141414101020304050607")
    )

    @Test
    fun testEncrypt() {
        for (vector in TEST_VECTORS) {
            val buffer = Unpooled.copiedBuffer(vector.plaintext)
            try {
                buffer.xteaEncrypt(0, buffer.readableBytes(), vector.key)

                val expected = Unpooled.wrappedBuffer(vector.ciphertext)
                try {
                    assertEquals(expected, buffer)
                } finally {
                    expected.release()
                }
            } finally {
                buffer.release()
            }
        }
    }

    @Test
    fun testDecrypt() {
        for (vector in TEST_VECTORS) {
            val buffer = Unpooled.copiedBuffer(vector.ciphertext)
            try {
                buffer.xteaDecrypt(0, buffer.readableBytes(), vector.key)

                val expected = Unpooled.wrappedBuffer(vector.plaintext)
                try {
                    assertEquals(expected, buffer)
                } finally {
                    expected.release()
                }
            } finally {
                buffer.release()
            }
        }
    }
}
