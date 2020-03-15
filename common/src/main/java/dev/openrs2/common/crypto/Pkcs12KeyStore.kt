package dev.openrs2.common.crypto

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
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.KeyStore
import java.time.OffsetDateTime
import java.time.Period
import java.time.ZoneOffset
import java.util.Date

class Pkcs12KeyStore private constructor(private val path: Path) {
    fun signJar(jar: Path) {
        exec(
            "jarsigner",
            "-keystore", path.toString(),
            "-storetype", "pkcs12",
            "-storepass", PASSWORD,
            "-keypass", PASSWORD,
            jar.toString(),
            ALIAS
        )
    }

    companion object {
        private const val ALIAS = "openrs2"

        private const val PASSWORD = ALIAS
        private val PASSWORD_CHARS = PASSWORD.toCharArray()
        private val PASSWORD_PARAMETER = KeyStore.PasswordProtection(PASSWORD_CHARS)

        private const val SERIAL_LENGTH = 128

        // TODO(gpe): add support for overriding this
        private val DNAME = X500NameBuilder()
            .addRDN(BCStyle.CN, "OpenRS2")
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

            if (!keyStore.containsAlias(ALIAS)) {
                keyStore.setEntry(ALIAS, createPrivateKeyEntry(), PASSWORD_PARAMETER)

                Files.newOutputStream(path).use { output ->
                    keyStore.store(output, PASSWORD_CHARS)
                }
            }

            return Pkcs12KeyStore(path)
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

        private fun exec(command: String, vararg args: String) {
            val commandWithArgs = listOf(command, *args)
            val status = ProcessBuilder(commandWithArgs)
                .start()
                .waitFor()
            if (status != 0) {
                throw IOException("$command returned non-zero status code $status")
            }
        }
    }
}
