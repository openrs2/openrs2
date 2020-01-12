package dev.openrs2.common.crypto

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
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import org.bouncycastle.util.io.pem.PemWriter
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path

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
