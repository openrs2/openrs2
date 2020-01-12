package dev.openrs2.common.crypto

import java.security.SecureRandom

private val threadLocal = ThreadLocal.withInitial { SecureRandom() }

val secureRandom: SecureRandom
    get() = threadLocal.get()
