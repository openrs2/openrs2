package org.openrs2.crypto

import com.fasterxml.jackson.databind.Module
import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters

public object CryptoModule : AbstractModule() {
    override fun configure() {
        bind(RSAPrivateCrtKeyParameters::class.java)
            .toProvider(RsaKeyProvider::class.java)
            .`in`(Scopes.SINGLETON)

        Multibinder.newSetBinder(binder(), Module::class.java)
            .addBinding().to(CryptoJacksonModule::class.java)
    }
}
