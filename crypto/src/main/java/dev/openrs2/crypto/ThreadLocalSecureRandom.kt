package dev.openrs2.crypto

import java.security.SecureRandom

private val threadLocal = ThreadLocal.withInitial { SecureRandom() }

val secureRandom: SecureRandom
    get() = threadLocal.get()
