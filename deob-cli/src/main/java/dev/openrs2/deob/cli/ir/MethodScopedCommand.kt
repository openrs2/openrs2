package dev.openrs2.deob.cli.ir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import dev.openrs2.deob.cli.DeobfuscatorOptions
import dev.openrs2.deob.ir.Method
import dev.openrs2.deob.ir.translation.BytecodeToIrTranlator

abstract class MethodScopedCommand(command: String) : CliktCommand(name = command) {
    val className: String by argument(help = "Fully qualified name of the class name to disassemble")
    val methodName: String by argument(help = "Name of the method to print the control flow graph for")
    val options by requireObject<DeobfuscatorOptions>()

    final override fun run() {
        val clazz = PrintCfgCommand.options.classLoader.load(PrintCfgCommand.className)
        val method = clazz.methods.find { it.name == PrintCfgCommand.methodName }!!

        val decompiler = BytecodeToIrTranlator()
        val ir = decompiler.decompile(clazz, method)

        run(ir)
    }

    abstract fun run(method: Method)
}
