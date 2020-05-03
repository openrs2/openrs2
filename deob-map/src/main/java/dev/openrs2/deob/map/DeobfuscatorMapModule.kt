package dev.openrs2.deob.map

import com.fasterxml.jackson.databind.Module
import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder
import dev.openrs2.yaml.YamlModule

object DeobfuscatorMapModule : AbstractModule() {
    override fun configure() {
        install(YamlModule)

        Multibinder.newSetBinder(binder(), Module::class.java)
            .addBinding().to(DeobfuscatorMapJacksonModule::class.java)

        bind(NameMap::class.java)
            .toProvider(NameMapProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }
}
