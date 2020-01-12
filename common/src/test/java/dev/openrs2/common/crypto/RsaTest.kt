package dev.openrs2.common.crypto

import io.netty.buffer.Unpooled
import org.bouncycastle.crypto.params.RSAKeyParameters
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters
import org.bouncycastle.util.Properties
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

object RsaTest {
    private const val ALLOW_UNSAFE_MOD = "org.bouncycastle.rsa.allow_unsafe_mod"

    @Test
    fun testGenerateKeyPair() {
        val (public, private) = Rsa.generateKeyPair()

        val expectedPlaintext = BigInteger("1337")
        val ciphertext = Rsa.encrypt(expectedPlaintext, public)
        val actualPlaintext = Rsa.decrypt(ciphertext, private)

        assertEquals(expectedPlaintext, actualPlaintext)
    }

    @Test
    fun testEncrypt() {
        // from https://en.wikipedia.org/wiki/RSA_(cryptosystem)#Example
        val public = allowUnsafeMod { RSAKeyParameters(false, BigInteger("3233"), BigInteger("17")) }
        val ciphertext = Rsa.encrypt(BigInteger("65"), public)
        assertEquals(BigInteger("2790"), ciphertext)
    }

    @Test
    fun testDecrypt() {
        // from https://en.wikipedia.org/wiki/RSA_(cryptosystem)#Example
        val public = allowUnsafeMod { RSAKeyParameters(true, BigInteger("3233"), BigInteger("413")) }
        val ciphertext = Rsa.decrypt(BigInteger("2790"), public)
        assertEquals(BigInteger("65"), ciphertext)
    }

    @Test
    fun testDecryptCrt() {
        // from https://en.wikipedia.org/wiki/RSA_(cryptosystem)#Example
        val private = allowUnsafeMod { RSAPrivateCrtKeyParameters(
            BigInteger("3233"), // modulus
            BigInteger("17"), // public exponent
            BigInteger("413"), // private exponent
            BigInteger("61"), // p
            BigInteger("53"), // q
            BigInteger("53"), // dP
            BigInteger("49"), // dQ
            BigInteger("38") // qInv
        ) }
        val ciphertext = Rsa.decrypt(BigInteger("2790"), private)
        assertEquals(BigInteger("65"), ciphertext)
    }

    @Test
    fun testEncryptByteBuf() {
        // from https://en.wikipedia.org/wiki/RSA_(cryptosystem)#Example
        val public = allowUnsafeMod { RSAKeyParameters(false, BigInteger("3233"), BigInteger("17")) }

        val plaintext = Unpooled.wrappedBuffer(byteArrayOf(65))
        try {
            val ciphertext = plaintext.rsaEncrypt(public)
            try {
                val expectedCiphertext = Unpooled.wrappedBuffer(byteArrayOf(10, 230.toByte()))
                try {
                    assertEquals(expectedCiphertext, ciphertext)
                } finally {
                    expectedCiphertext.release()
                }
            } finally {
                ciphertext.release()
            }
        } finally {
            plaintext.release()
        }
    }

    @Test
    fun testDecryptByteBuf() {
        // from https://en.wikipedia.org/wiki/RSA_(cryptosystem)#Example
        val private = allowUnsafeMod { RSAKeyParameters(true, BigInteger("3233"), BigInteger("413")) }

        val ciphertext = Unpooled.wrappedBuffer(byteArrayOf(10, 230.toByte()))
        try {
            val plaintext = ciphertext.rsaDecrypt(private)
            try {
                val expectedPlaintext = Unpooled.wrappedBuffer(byteArrayOf(65))
                try {
                    assertEquals(expectedPlaintext, plaintext)
                } finally {
                    expectedPlaintext.release()
                }
            } finally {
                plaintext.release()
            }
        } finally {
            ciphertext.release()
        }
    }

    private fun <T> allowUnsafeMod(f: () -> T): T {
        Properties.setThreadOverride(ALLOW_UNSAFE_MOD, true)
        try {
            return f()
        } finally {
            Properties.setThreadOverride(ALLOW_UNSAFE_MOD, false)
        }
    }
}
