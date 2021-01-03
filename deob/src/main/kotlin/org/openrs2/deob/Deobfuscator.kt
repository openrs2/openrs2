package org.openrs2.deob

import com.github.michaelbull.logging.InlineLogger
import org.openrs2.decompiler.Decompiler
import org.openrs2.deob.ast.AstDeobfuscator
import org.openrs2.deob.bytecode.BytecodeDeobfuscator
import org.openrs2.deob.util.Module
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class Deobfuscator @Inject constructor(
    private val bytecodeDeobfuscator: BytecodeDeobfuscator,
    private val decompiler: Decompiler,
    private val astDeobfuscator: AstDeobfuscator
) {
    public fun run() {
        logger.info { "Deobfuscating bytecode" }
        bytecodeDeobfuscator.run(
            input = Paths.get("nonfree/lib"),
            output = Paths.get("nonfree/var/cache/deob")
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
