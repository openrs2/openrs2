package dev.openrs2.asm

import com.fasterxml.jackson.databind.Module
import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import dev.openrs2.yaml.YamlModule

object AsmModule : AbstractModule() {
    override fun configure() {
        install(YamlModule)

        Multibinder.newSetBinder(binder(), Module::class.java)
            .addBinding().to(AsmJacksonModule::class.java)
    }
}
