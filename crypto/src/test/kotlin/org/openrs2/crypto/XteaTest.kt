package org.openrs2.crypto

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.openrs2.buffer.use
import kotlin.test.Test
import kotlin.test.assertEquals

class XteaTest {
    private class TestVector(key: String, plaintext: String, ciphertext: String) {
        val key = XteaKey.fromHex(key)
        val plaintext: ByteArray = ByteBufUtil.decodeHexDump(plaintext)
        val ciphertext: ByteArray = ByteBufUtil.decodeHexDump(ciphertext)
    }

    @Test
    fun testEncrypt() {
        for (vector in TEST_VECTORS) {
            for (i in 0..7) {
                for (j in 0..7) {
                    val header = ByteArray(i) { it.toByte() }
                    val trailer = ByteArray(j) { it.toByte() }

                    Unpooled.copiedBuffer(header, vector.plaintext, trailer).use { buffer ->
                        buffer.xteaEncrypt(i, vector.plaintext.size, vector.key)

                        Unpooled.wrappedBuffer(header, vector.ciphertext, trailer).use { expected ->
                            assertEquals(expected, buffer)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testDecrypt() {
        for (vector in TEST_VECTORS) {
            for (i in 0..7) {
                for (j in 0..7) {
                    val header = ByteArray(i) { it.toByte() }
                    val trailer = ByteArray(j) { it.toByte() }

                    Unpooled.copiedBuffer(header, vector.ciphertext, trailer).use { buffer ->
                        buffer.xteaDecrypt(i, vector.ciphertext.size, vector.key)

                        Unpooled.wrappedBuffer(header, vector.plaintext, trailer).use { expected ->
                            assertEquals(expected, buffer)
                        }
                    }
                }
            }
        }
    }

    private companion object {
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
            TestVector(
                "00000000000000000000000000000000", "70e1225d6e4e76554141414141414141",
                "4141414141414141ed23375a821a8c2d"
            )
        )
    }
}
