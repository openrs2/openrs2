package dev.openrs2.common.crypto

import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Provider

class RsaKeyProvider : Provider<RSAPrivateCrtKeyParameters> {
    override fun get(): RSAPrivateCrtKeyParameters {
        return if (Files.exists(PATH)) {
            Rsa.readPrivateKey(PATH)
        } else {
            val (_, private) = Rsa.generateKeyPair()
            Rsa.writePrivateKey(PATH, private)
            private
        }
    }

    companion object {
        private val PATH = Paths.get("conf/private.key")
    }
}
