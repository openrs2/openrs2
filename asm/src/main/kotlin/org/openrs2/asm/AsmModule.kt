package org.openrs2.asm

import com.fasterxml.jackson.databind.Module
import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import org.openrs2.buffer.BufferModule

public object AsmModule : AbstractModule() {
    override fun configure() {
        install(BufferModule)

        Multibinder.newSetBinder(binder(), Module::class.java)
            .addBinding().to(AsmJacksonModule::class.java)
    }
}
