package dev.openrs2

import kotlin.system.exitProcess
import dev.openrs2.bundler.main as bundlerMain
import dev.openrs2.decompiler.main as decompilerMain
import dev.openrs2.deob.ast.main as astDeobfuscatorMain
import dev.openrs2.deob.main as deobfuscatorMain
import dev.openrs2.game.main as gameMain

fun main(args: Array<String>) {
    val command: String
    val commandArgs: Array<String>
    if (args.isEmpty()) {
        command = "game"
        commandArgs = emptyArray()
    } else {
        command = args[0]
        commandArgs = args.copyOfRange(1, args.size)
    }

    when (command) {
        "bundle" -> bundlerMain()
        "decompile" -> decompilerMain()
        "deob" -> deobfuscatorMain()
        "deob-ast" -> astDeobfuscatorMain()
        "game" -> gameMain()
        else -> {
            System.err.println("Usage: openrs2 [<command> [<args>]]")
            System.err.println()
            System.err.println("Commands:")
            System.err.println("  bundle")
            System.err.println("  decompile")
            System.err.println("  deob")
            System.err.println("  deob-ast")
            System.err.println("  game")
            exitProcess(1)
        }
    }
}
