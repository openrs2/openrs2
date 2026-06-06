package org.openrs2.deob

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.decompiler.Decompiler
import org.openrs2.deob.ast.AstDeobfuscator
import org.openrs2.deob.bytecode.BytecodeDeobfuscator

@Singleton
public class Deobfuscator @Inject constructor(
    private val bytecodeDeobfuscator: BytecodeDeobfuscator,
    private val decompiler: Decompiler,
    private val astDeobfuscator: AstDeobfuscator
) {
    public fun run() {
        logger.info { "Deobfuscating bytecode" }
        bytecodeDeobfuscator.run()

        logger.info { "Decompiling" }
        decompiler.run()

        logger.info { "Deobfuscating AST" }
        astDeobfuscator.run()
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
