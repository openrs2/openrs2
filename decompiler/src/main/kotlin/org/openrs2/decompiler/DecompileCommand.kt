package org.openrs2.decompiler

import com.github.ajalt.clikt.core.CliktCommand
import org.openrs2.deob.util.Module

public fun main(args: Array<String>): Unit = DecompileCommand().main(args)

public class DecompileCommand : CliktCommand(name = "decompile") {
    override fun run() {
        // val decompiler = Decompiler()
        // decompiler.run(Module.ALL)
    }
}
