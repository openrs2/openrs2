package org.openrs2.deob

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.decompiler.Decompiler
import org.openrs2.deob.ast.AstDeobfuscator
import org.openrs2.deob.bytecode.BytecodeDeobfuscator
import org.openrs2.deob.bytecode.Profile
import org.openrs2.deob.util.Module
import java.nio.file.Path

@Singleton
public class Deobfuscator @Inject constructor(
    private val profile: Profile,
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

        val all = mutableMapOf<String, Module>()

        profile.libraries.forEach { lib ->
            val name = lib.key

            all += name to Module(name)
        }

        profile.libraries.forEach { lib ->
            val name = lib.key
            val conf = lib.value

            if (!conf.requires.isNullOrEmpty()) {
                val requires = conf.requires!!.map { all.getValue(it) }.toSet()

                all += name to Module(name, requires)
            }
        }

        logger.info { "Decompiling" }
        decompiler.run(all.values.toSet())

        logger.info { "Deobfuscating AST" }
        astDeobfuscator.run(all.values.toSet())
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
