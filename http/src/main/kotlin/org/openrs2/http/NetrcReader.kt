package org.openrs2.http

import java.io.IOException
import java.net.PasswordAuthentication
import java.util.Scanner
import java.util.regex.Pattern

public class NetrcReader(private val scanner: Scanner) {
    private enum class State {
        DEFAULT,
        READ_MACHINE
    }

    private val hosts = mutableMapOf<String, PasswordAuthentication>()
    private var default: PasswordAuthentication? = null

    private var host: String? = null
    private var username: String? = null
    private var password: String? = null

    private var state = State.DEFAULT
        set(value) {
            if (field != State.DEFAULT) {
                val auth = PasswordAuthentication(username ?: "", password?.toCharArray() ?: EMPTY_CHAR_ARRAY)

                val host = host
                if (host != null) {
                    hosts[host] = auth
                } else if (default == null) {
                    default = auth
                }
            }

            field = value
        }

    init {
        scanner.useDelimiter(WHITESPACE)
    }

    public fun read(): NetrcAuthenticator {
        while (scanner.hasNext()) {
            when (val token = scanner.next()) {
                "machine" -> {
                    if (!scanner.hasNext()) {
                        throw IOException("Expected hostname")
                    }

                    state = State.READ_MACHINE
                    host = scanner.next()
                }

                "default" -> {
                    state = State.READ_MACHINE
                    host = null
                }

                "login", "password", "account" -> {
                    if (state != State.READ_MACHINE) {
                        throw IOException("Unexpected token '$token'")
                    } else if (!scanner.hasNext()) {
                        throw IOException("Expected $token")
                    }

                    if (token == "login") {
                        username = scanner.next()
                    } else if (token == "password") {
                        password = scanner.next()
                    }
                }

                "macdef" -> skipMacro()
            }
        }

        // trigger the logic to add the final machine to the map
        state = State.DEFAULT

        return NetrcAuthenticator(hosts, default)
    }

    private fun skipMacro() {
        if (!scanner.hasNext()) {
            throw IOException("Expected macro name")
        }

        while (scanner.hasNextLine() && scanner.nextLine().isNotEmpty()) {
            // empty
        }

        state = State.DEFAULT
    }

    private companion object {
        private val WHITESPACE = Pattern.compile("\\s+")
        private val EMPTY_CHAR_ARRAY = charArrayOf()
    }
}
