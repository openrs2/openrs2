package dev.openrs2.deob.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.openrs2.deob.cli.ir.PrintCfgCommand
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class DeobfuscatorCli : CliktCommand(name = "deob") {
    val classpath: String by option(help = "Change the classpath used to resolve class files")
        .default("system")

    override fun run() {
        val loader = when (classpath) {
            "system" -> SystemClassLoader
            else -> {
                val paths : List<Path> = classpath.split(File.pathSeparatorChar).map {
                    Paths.get(it)
                }

                val classLoader = ClasspathClassLoader(paths)

                classLoader
            }
        }

        currentContext.obj = DeobfuscatorOptions(loader)
    }
}

fun main(args: Array<String>) = DeobfuscatorCli()
    .subcommands(PrintCfgCommand)
    .main(args)
