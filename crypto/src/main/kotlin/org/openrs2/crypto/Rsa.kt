package org.openrs2.crypto

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.pkcs.RSAPrivateKey
import org.bouncycastle.asn1.pkcs.RSAPublicKey
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters
import org.bouncycastle.crypto.params.RSAKeyParameters
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory
import org.bouncycastle.util.BigIntegers
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import org.bouncycastle.util.io.pem.PemWriter
import org.openrs2.util.io.useAtomicBufferedWriter
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.spec.KeySpec
import java.security.spec.RSAPrivateCrtKeySpec

public val RSAPrivateCrtKeyParameters.publicKey: RSAKeyParameters
    get() = RSAKeyParameters(false, modulus, publicExponent)

private fun ByteBuf.toBigInteger(): BigInteger {
    val bytes = ByteBufUtil.getBytes(this, readerIndex(), readableBytes(), false)
    return BigInteger(bytes)
}

private fun BigInteger.toByteBuf(): ByteBuf {
    return Unpooled.wrappedBuffer(toByteArray())
}

public fun ByteBuf.rsaCrypt(key: RSAKeyParameters): ByteBuf {
    return Rsa.crypt(toBigInteger(), key).toByteBuf()
}

public fun RSAPrivateCrtKeyParameters.toKeySpec(): KeySpec {
    return RSAPrivateCrtKeySpec(modulus, publicExponent, exponent, p, q, dp, dq, qInv)
}

public object Rsa {
    private const val PUBLIC_KEY = "PUBLIC KEY"
    private const val PRIVATE_KEY = "PRIVATE KEY"

    private val F4 = BigInteger("65537")

    /*
     * The client writes the output of RSA to a 128 byte buffer. It prefixes
     * the output with a single length byte, leaving 127 bytes for the actual
     * RSA output.
     *
     * The maximum output length of RSA encryption is the key size plus one, so
     * the maximum key size supported by the client is 126 bytes - or 1008 bits.
     */
    public const val CLIENT_KEY_LENGTH: Int = 1008

    public const val JAR_KEY_LENGTH: Int = 2048

    // 1 in 2^80
    private const val CERTAINTY = 80

    /*
     * The magic number prepended as a byte to the plaintext before it is
     * encrypted by the server.
     */
    public const val MAGIC: Int = 10

    public fun generateKeyPair(length: Int): Pair<RSAKeyParameters, RSAPrivateCrtKeyParameters> {
        val generator = RSAKeyPairGenerator()
        generator.init(RSAKeyGenerationParameters(F4, secureRandom, length, CERTAINTY))

        val keyPair = generator.generateKeyPair()
        return Pair(keyPair.public as RSAKeyParameters, keyPair.private as RSAPrivateCrtKeyParameters)
    }

    private fun generateBlindingFactor(m: BigInteger): Pair<BigInteger, BigInteger> {
        val max = m - BigInteger.ONE

        while (true) {
            val r = BigIntegers.createRandomInRange(BigInteger.ONE, max, secureRandom)
            val rInv = try {
                r.modInverse(m)
            } catch (ex: ArithmeticException) {
                continue
            }
            return Pair(r, rInv)
        }
    }

    public fun crypt(ciphertext: BigInteger, key: RSAKeyParameters): BigInteger {
        if (key is RSAPrivateCrtKeyParameters) {
            // blind the input
            val e = key.publicExponent
            val m = key.modulus
            val (r, rInv) = generateBlindingFactor(m)

            val blindCiphertext = (r.modPow(e, m) * ciphertext).mod(m)

            // decrypt using the Chinese Remainder Theorem
            val p = key.p
            val q = key.q
            val dP = key.dp
            val dQ = key.dq
            val qInv = key.qInv

            val mP = (blindCiphertext.mod(p)).modPow(dP, p)
            val mQ = (blindCiphertext.mod(q)).modPow(dQ, q)

            val h = (qInv * (mP - mQ)).mod(p)

            val blindPlaintext = (h * q) + mQ

            // unblind output
            val plaintext = (blindPlaintext * rInv).mod(m)

            // defend against CRT faults (see https://people.redhat.com/~fweimer/rsa-crt-leaks.pdf)
            check(plaintext.modPow(e, m) == ciphertext)

            return plaintext
        } else {
            return ciphertext.modPow(key.exponent, key.modulus)
        }
    }

    public fun readPublicKey(path: Path): RSAKeyParameters {
        val der = readSinglePemObject(path, PUBLIC_KEY)

        val spki = SubjectPublicKeyInfo.getInstance(der)
        validateAlgorithm(spki.algorithm)

        val key = RSAPublicKey.getInstance(spki.parsePublicKey())
        return RSAKeyParameters(false, key.modulus, key.publicExponent)
    }

    public fun writePublicKey(path: Path, key: RSAKeyParameters) {
        val spki = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(key)
        return writeSinglePemObject(path, PUBLIC_KEY, spki.encoded)
    }

    public fun readPrivateKey(path: Path): RSAPrivateCrtKeyParameters {
        val der = readSinglePemObject(path, PRIVATE_KEY)

        val pki = PrivateKeyInfo.getInstance(der)
        validateAlgorithm(pki.privateKeyAlgorithm)

        val key = RSAPrivateKey.getInstance(pki.parsePrivateKey())
        return RSAPrivateCrtKeyParameters(
            key.modulus,
            key.publicExponent,
            key.privateExponent,
            key.prime1,
            key.prime2,
            key.exponent1,
            key.exponent2,
            key.coefficient
        )
    }

    public fun writePrivateKey(path: Path, key: RSAKeyParameters) {
        val pki = PrivateKeyInfoFactory.createPrivateKeyInfo(key)
        return writeSinglePemObject(path, PRIVATE_KEY, pki.encoded)
    }

    private fun validateAlgorithm(id: AlgorithmIdentifier) {
        if (id.algorithm != PKCSObjectIdentifiers.rsaEncryption) {
            throw IOException("Invalid algorithm identifier, expecting rsaEncryption")
        }

        if (id.parameters != DERNull.INSTANCE) {
            throw IOException("Invalid algorithm parameters, expecting NULL")
        }
    }

    private fun readSinglePemObject(path: Path, type: String): ByteArray {
        PemReader(Files.newBufferedReader(path)).use {
            val obj = it.readPemObject()
            if (obj == null || obj.type != type || it.readPemObject() != null) {
                throw IOException("Expecting single $type PEM object")
            }

            if (obj.headers.isNotEmpty()) {
                throw IOException("PEM headers unsupported")
            }

            return obj.content
        }
    }

    private fun writeSinglePemObject(path: Path, type: String, content: ByteArray) {
        path.useAtomicBufferedWriter { writer ->
            PemWriter(writer).use {
                it.writeObject(PemObject(type, content))
            }
        }
    }
}
