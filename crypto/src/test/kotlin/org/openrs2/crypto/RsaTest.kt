package org.openrs2.crypto

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.bouncycastle.crypto.params.RSAKeyParameters
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters
import org.bouncycastle.util.Properties
import org.openrs2.buffer.use
import org.openrs2.buffer.wrappedBuffer
import org.openrs2.util.io.useTempFile
import java.math.BigInteger
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object RsaTest {
    private const val ALLOW_UNSAFE_MOD = "org.bouncycastle.rsa.allow_unsafe_mod"

    // example data from https://en.wikipedia.org/wiki/RSA_(cryptosystem)#Example
    private val PUBLIC_KEY = allowUnsafeMod { RSAKeyParameters(false, BigInteger("3233"), BigInteger("17")) }
    private val PRIVATE_KEY = allowUnsafeMod { RSAKeyParameters(true, BigInteger("3233"), BigInteger("413")) }
    private val PRIVATE_KEY_CRT = allowUnsafeMod {
        RSAPrivateCrtKeyParameters(
            BigInteger("3233"), // modulus
            BigInteger("17"), // public exponent
            BigInteger("413"), // private exponent
            BigInteger("61"), // p
            BigInteger("53"), // q
            BigInteger("53"), // dP
            BigInteger("49"), // dQ
            BigInteger("38") // qInv
        )
    }

    private val PUBLIC_KEY_PEM = listOf(
        "-----BEGIN PUBLIC KEY-----",
        "MBswDQYJKoZIhvcNAQEBBQADCgAwBwICDKECARE=",
        "-----END PUBLIC KEY-----"
    )
    private val PRIVATE_KEY_PEM = listOf(
        "-----BEGIN PRIVATE KEY-----",
        "MDMCAQAwDQYJKoZIhvcNAQEBBQAEHzAdAgEAAgIMoQIBEQICAZ0CAT0CATUCATUC",
        "ATECASY=",
        "-----END PRIVATE KEY-----"
    )

    @Test
    fun testGenerateKeyPair() {
        val (public, private) = Rsa.generateKeyPair(Rsa.CLIENT_KEY_LENGTH)

        val expectedPlaintext = BigInteger("1337")
        val ciphertext = Rsa.apply(expectedPlaintext, public)
        val actualPlaintext = Rsa.apply(ciphertext, private)

        assertEquals(expectedPlaintext, actualPlaintext)
    }

    @Test
    fun testEncrypt() {
        val ciphertext = Rsa.apply(BigInteger("65"), PUBLIC_KEY)
        assertEquals(BigInteger("2790"), ciphertext)
    }

    @Test
    fun testDecrypt() {
        val ciphertext = Rsa.apply(BigInteger("2790"), PRIVATE_KEY)
        assertEquals(BigInteger("65"), ciphertext)
    }

    @Test
    fun testDecryptCrt() {
        val ciphertext = Rsa.apply(BigInteger("2790"), PRIVATE_KEY_CRT)
        assertEquals(BigInteger("65"), ciphertext)
    }

    @Test
    fun testEncryptByteBuf() {
        wrappedBuffer(65).use { plaintext ->
            plaintext.rsa(PUBLIC_KEY).use { ciphertext ->
                wrappedBuffer(10, 230.toByte()).use { expectedCiphertext ->
                    assertEquals(expectedCiphertext, ciphertext)
                }
            }
        }
    }

    @Test
    fun testDecryptByteBuf() {
        wrappedBuffer(10, 230.toByte()).use { ciphertext ->
            ciphertext.rsa(PRIVATE_KEY).use { plaintext ->
                wrappedBuffer(65).use { expectedPlaintext ->
                    assertEquals(expectedPlaintext, plaintext)
                }
            }
        }
    }

    @Test
    fun testReadPublicKey() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            fs.getPath("/").useTempFile("public", ".key") { file ->
                Files.write(file, PUBLIC_KEY_PEM)

                val key = allowUnsafeMod { Rsa.readPublicKey(file) }
                assertFalse(key.isPrivate)
                assertEquals(PUBLIC_KEY.modulus, key.modulus)
                assertEquals(PUBLIC_KEY.exponent, key.exponent)
            }
        }
    }

    @Test
    fun testWritePublicKey() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            fs.getPath("/").useTempFile("public", ".key") { file ->
                Rsa.writePublicKey(file, PUBLIC_KEY)

                assertEquals(PUBLIC_KEY_PEM, Files.readAllLines(file))
            }
        }
    }

    @Test
    fun testReadPrivateKey() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            fs.getPath("/").useTempFile("private", ".key") { file ->
                Files.write(file, PRIVATE_KEY_PEM)

                val key = allowUnsafeMod { Rsa.readPrivateKey(file) }
                assertTrue(key.isPrivate)
                assertEquals(PRIVATE_KEY_CRT.modulus, key.modulus)
                assertEquals(PRIVATE_KEY_CRT.exponent, key.exponent)
                assertEquals(PRIVATE_KEY_CRT.publicExponent, key.publicExponent)
                assertEquals(PRIVATE_KEY_CRT.p, key.p)
                assertEquals(PRIVATE_KEY_CRT.q, key.q)
                assertEquals(PRIVATE_KEY_CRT.dp, key.dp)
                assertEquals(PRIVATE_KEY_CRT.dq, key.dq)
                assertEquals(PRIVATE_KEY_CRT.qInv, key.qInv)
            }
        }
    }

    @Test
    fun testWritePrivateKey() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            fs.getPath("/").useTempFile("private", ".key") { file ->
                Rsa.writePrivateKey(file, PRIVATE_KEY_CRT)

                assertEquals(PRIVATE_KEY_PEM, Files.readAllLines(file))
            }
        }
    }

    @Test
    fun testPrivateToPublicKey() {
        val public = allowUnsafeMod { PRIVATE_KEY_CRT.publicKey }
        assertEquals(PUBLIC_KEY.modulus, public.modulus)
        assertEquals(PUBLIC_KEY.exponent, public.exponent)
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
