package org.openrs2.http

import com.google.common.base.StandardSystemProperty
import java.io.IOException
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Scanner

public class NetrcAuthenticator(
    private val hosts: Map<String, PasswordAuthentication>,
    private val default: PasswordAuthentication?
) : Authenticator() {
    override fun getPasswordAuthentication(): PasswordAuthentication? {
        return hosts.getOrDefault(requestingHost, default)
    }

    public companion object {
        private val EMPTY_AUTHENTICATOR = NetrcAuthenticator(emptyMap(), null)

        public fun read(): NetrcAuthenticator {
            val home = StandardSystemProperty.USER_HOME.value() ?: throw IOException("user.home not defined")
            val path = Paths.get(home, ".netrc")

            return if (Files.exists(path)) {
                Scanner(path, Charsets.UTF_8).use { scanner ->
                    read(scanner)
                }
            } else {
                EMPTY_AUTHENTICATOR
            }
        }

        public fun read(scanner: Scanner): NetrcAuthenticator {
            return NetrcReader(scanner).read()
        }
    }
}
