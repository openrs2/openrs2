package dev.openrs2.crypto

import java.security.SecureRandom

private val threadLocal = ThreadLocal.withInitial { SecureRandom() }

public val secureRandom: SecureRandom
    get() = threadLocal.get()
