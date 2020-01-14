package dev.openrs2.common

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import dev.openrs2.common.crypto.RsaKeyProvider
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters

class CommonModule : AbstractModule() {
    override fun configure() {
        bind(RSAPrivateCrtKeyParameters::class.java)
            .toProvider(RsaKeyProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }
}
