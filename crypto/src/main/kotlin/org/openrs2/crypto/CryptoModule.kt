package org.openrs2.crypto

import com.fasterxml.jackson.databind.Module
import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder

public object CryptoModule : AbstractModule() {
    override fun configure() {
        Multibinder.newSetBinder(binder(), Module::class.java)
            .addBinding().to(CryptoJacksonModule::class.java)
    }
}
