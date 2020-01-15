package dev.openrs2.common.crypto

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class Pkcs12KeyStore private constructor(private val path: Path) {
    fun signJar(jar: Path) {
        exec(
            "jarsigner",
            "-keystore", path.toString(),
            "-storetype", "pkcs12",
            "-storepass", STORE_PASSWORD,
            "-keypass", KEY_PASSWORD,
            jar.toString(),
            ALIAS
        )
    }

    companion object {
        private const val ALIAS = "openrs2"
        private const val STORE_PASSWORD = ALIAS
        private const val KEY_PASSWORD = ALIAS
        private const val DNAME = "CN=OpenRS2"
        private const val VALIDITY_PERIOD = "3650"
        private const val KEY_ALGORITHM = "RSA"
        private const val KEY_SIZE = "2048"
        private const val SIGNATURE_ALGORITHM = "SHA256with$KEY_ALGORITHM"

        fun open(path: Path): Pkcs12KeyStore {
            if (!Files.exists(path)) {
                create(path)
            }
            return Pkcs12KeyStore(path)
        }

        private fun create(path: Path) {
            exec(
                "keytool",
                "-genkeypair",
                "-keystore", path.toString(),
                "-storetype", "pkcs12",
                "-storepass", STORE_PASSWORD,
                "-keypass", KEY_PASSWORD,
                "-alias", ALIAS,
                "-dname", DNAME,
                "-validity", VALIDITY_PERIOD,
                "-keyalg", KEY_ALGORITHM,
                "-keysize", KEY_SIZE,
                "-sigalg", SIGNATURE_ALGORITHM
            )
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
