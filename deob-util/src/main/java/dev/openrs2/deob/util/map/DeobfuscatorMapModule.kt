package dev.openrs2.deob.util.map

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import dev.openrs2.asm.AsmModule

object DeobfuscatorMapModule : AbstractModule() {
    override fun configure() {
        install(AsmModule)

        bind(NameMap::class.java)
            .toProvider(NameMapProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }
}
