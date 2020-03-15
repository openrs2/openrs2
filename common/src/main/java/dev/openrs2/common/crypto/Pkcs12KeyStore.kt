package dev.openrs2.common.crypto

import jdk.security.jarsigner.JarSigner
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder
import org.bouncycastle.util.BigIntegers
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.KeyStore
import java.time.OffsetDateTime
import java.time.Period
import java.time.ZoneOffset
import java.util.Date
import java.util.jar.JarFile

class Pkcs12KeyStore private constructor(privateKeyEntry: KeyStore.PrivateKeyEntry) {
    private val signer = JarSigner.Builder(privateKeyEntry)
        .signatureAlgorithm("SHA256withRSA")
        .digestAlgorithm("SHA-256")
        .signerName(SIGNER_NAME)
        .build()

    fun signJar(input: Path, output: Path) {
        JarFile(input.toFile()).use { file ->
            Files.newOutputStream(output).use { os ->
                signer.sign(file, os)
            }
        }
    }

    companion object {
        private const val ALIAS = "openrs2"

        private const val PASSWORD = ALIAS
        private val PASSWORD_CHARS = PASSWORD.toCharArray()
        private val PASSWORD_PARAMETER = KeyStore.PasswordProtection(PASSWORD_CHARS)

        private const val SERIAL_LENGTH = 128

        // TODO(gpe): add support for overriding this
        private const val SIGNER_NAME = "OpenRS2"
        private val DNAME = X500NameBuilder()
            .addRDN(BCStyle.CN, SIGNER_NAME)
            .build()

        private val MAX_CLOCK_SKEW = Period.ofDays(1)
        private val VALIDITY_PERIOD = Period.ofYears(1)

        private val SHA256_WITH_RSA = AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption)
        private val SHA256 = AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256)

        fun open(path: Path): Pkcs12KeyStore {
            val keyStore = KeyStore.getInstance("PKCS12")
            if (Files.exists(path)) {
                Files.newInputStream(path).use { input ->
                    keyStore.load(input, PASSWORD_CHARS)
                }
            } else {
                keyStore.load(null)
            }

            val privateKeyEntry = if (keyStore.containsAlias(ALIAS)) {
                keyStore.getEntry(ALIAS, PASSWORD_PARAMETER) as KeyStore.PrivateKeyEntry
            } else {
                val entry = createPrivateKeyEntry()
                keyStore.setEntry(ALIAS, entry, PASSWORD_PARAMETER)

                Files.newOutputStream(path).use { output ->
                    keyStore.store(output, PASSWORD_CHARS)
                }

                entry
            }

            return Pkcs12KeyStore(privateKeyEntry)
        }

        private fun createPrivateKeyEntry(): KeyStore.PrivateKeyEntry {
            val (public, private) = Rsa.generateKeyPair(Rsa.JAR_KEY_LENGTH)

            val serial = BigIntegers.createRandomBigInteger(SERIAL_LENGTH, secureRandom)

            val start = OffsetDateTime.now(ZoneOffset.UTC).minus(MAX_CLOCK_SKEW)
            val end = start.plus(VALIDITY_PERIOD)

            val spki = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(public)
            val signer = BcRSAContentSignerBuilder(SHA256_WITH_RSA, SHA256).build(private)

            val certificate = X509v3CertificateBuilder(
                DNAME,
                serial,
                Date.from(start.toInstant()),
                Date.from(end.toInstant()),
                DNAME,
                spki
            ).build(signer)

            val jcaPrivate = KeyFactory.getInstance("RSA").generatePrivate(private.toKeySpec())
            val jcaCertificate = JcaX509CertificateConverter().getCertificate(certificate)
            return KeyStore.PrivateKeyEntry(jcaPrivate, arrayOf(jcaCertificate))
        }
    }
}
