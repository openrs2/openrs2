package org.openrs2.conf

import jakarta.inject.Provider
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters
import org.openrs2.crypto.Rsa
import java.nio.file.Files
import java.nio.file.Path

public class RsaKeyProvider : Provider<RSAPrivateCrtKeyParameters> {
    override fun get(): RSAPrivateCrtKeyParameters {
        return if (Files.exists(PATH)) {
            Rsa.readPrivateKey(PATH)
        } else {
            val (_, private) = Rsa.generateKeyPair(Rsa.CLIENT_KEY_LENGTH)
            Rsa.writePrivateKey(PATH, private)
            private
        }
    }

    private companion object {
        private val PATH = Path.of("etc/game.key")
    }
}
