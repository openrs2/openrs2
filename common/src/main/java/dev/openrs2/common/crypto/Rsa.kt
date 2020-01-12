package dev.openrs2.common.crypto

import io.netty.buffer.ByteBuf
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
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path

private fun ByteBuf.toBigInteger(): BigInteger {
    val bytes: ByteArray
    if (hasArray() && arrayOffset() == 0 && readerIndex() == 0 && readableBytes() == array().size) {
        bytes = array()
    } else {
        bytes = ByteArray(readableBytes())
        getBytes(readerIndex(), bytes)
    }

    return BigInteger(bytes)
}

private fun BigInteger.toByteBuf(): ByteBuf {
    return Unpooled.wrappedBuffer(toByteArray())
}

fun ByteBuf.rsaEncrypt(key: RSAKeyParameters): ByteBuf {
    return Rsa.encrypt(toBigInteger(), key).toByteBuf()
}

fun ByteBuf.rsaDecrypt(key: RSAKeyParameters): ByteBuf {
    return Rsa.decrypt(toBigInteger(), key).toByteBuf()
}

object Rsa {
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
    private const val KEY_LENGTH = 1008

    // 1 in 2^80
    private const val CERTAINTY = 80

    val RSAPrivateCrtKeyParameters.publicKey
        get() = RSAKeyParameters(false, modulus, publicExponent)

    fun generateKeyPair(): Pair<RSAKeyParameters, RSAPrivateCrtKeyParameters> {
        val generator = RSAKeyPairGenerator()
        generator.init(RSAKeyGenerationParameters(F4, secureRandom, KEY_LENGTH, CERTAINTY))

        val keyPair = generator.generateKeyPair()
        return Pair(keyPair.public as RSAKeyParameters, keyPair.private as RSAPrivateCrtKeyParameters)
    }

    fun encrypt(plaintext: BigInteger, key: RSAKeyParameters): BigInteger {
        require(!key.isPrivate)
        return plaintext.modPow(key.exponent, key.modulus)
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

    fun decrypt(ciphertext: BigInteger, key: RSAKeyParameters): BigInteger {
        require(key.isPrivate)

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
            if (plaintext.modPow(e, m) != ciphertext) {
                throw IllegalStateException()
            }

            return plaintext
        } else {
            return ciphertext.modPow(key.exponent, key.modulus)
        }
    }

    fun readPublicKey(path: Path): RSAKeyParameters {
        val der = readSinglePemObject(path, PUBLIC_KEY)

        val spki = SubjectPublicKeyInfo.getInstance(der)
        validateAlgorithm(spki.algorithm)

        val key = RSAPublicKey.getInstance(spki.parsePublicKey())
        return RSAKeyParameters(false, key.modulus, key.publicExponent)
    }

    fun writePublicKey(path: Path, key: RSAKeyParameters) {
        val spki = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(key)
        return writeSinglePemObject(path, PUBLIC_KEY, spki.encoded)
    }

    fun readPrivateKey(path: Path): RSAKeyParameters {
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

    fun writePrivateKey(path: Path, key: RSAKeyParameters) {
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
        PemWriter(Files.newBufferedWriter(path)).use {
            it.writeObject(PemObject(type, content))
        }
    }
}
