package org.openrs2.deob

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.decompiler.Decompiler
import org.openrs2.deob.ast.AstDeobfuscator
import org.openrs2.deob.bytecode.BytecodeDeobfuscator
import org.openrs2.deob.util.Module
import java.nio.file.Path

@Singleton
public class Deobfuscator @Inject constructor(
    private val bytecodeDeobfuscator: BytecodeDeobfuscator,
    private val decompiler: Decompiler,
    private val astDeobfuscator: AstDeobfuscator
) {
    public fun run() {
        logger.info { "Deobfuscating bytecode" }
        bytecodeDeobfuscator.run(
            input = Path.of("nonfree/lib"),
            output = Path.of("nonfree/var/cache/deob")
        )

        logger.info { "Decompiling" }
        decompiler.run(Module.ALL)

        logger.info { "Deobfuscating AST" }
        astDeobfuscator.run(Module.ALL)
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
