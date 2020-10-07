package org.openrs2.deob.util

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import org.openrs2.asm.AsmModule
import org.openrs2.deob.util.map.NameMap
import org.openrs2.deob.util.map.NameMapProvider

public object DeobfuscatorUtilModule : AbstractModule() {
    override fun configure() {
        install(AsmModule)

        bind(NameMap::class.java)
            .toProvider(NameMapProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }
}
