package org.openrs2.asm

import com.fasterxml.jackson.databind.Module
import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder

public object AsmModule : AbstractModule() {
    override fun configure() {
        Multibinder.newSetBinder(binder(), Module::class.java)
            .addBinding().to(AsmJacksonModule::class.java)
    }
}
