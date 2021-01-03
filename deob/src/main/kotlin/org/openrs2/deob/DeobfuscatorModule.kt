package org.openrs2.deob

import com.google.inject.AbstractModule
import org.openrs2.deob.ast.AstDeobfuscatorModule
import org.openrs2.deob.bytecode.BytecodeDeobfuscatorModule

public object DeobfuscatorModule : AbstractModule() {
    override fun configure() {
        install(AstDeobfuscatorModule)
        install(BytecodeDeobfuscatorModule)
    }
}
