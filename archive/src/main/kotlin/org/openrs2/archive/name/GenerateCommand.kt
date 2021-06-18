package org.openrs2.archive.name

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.inject.CloseableInjector

public class GenerateCommand : CliktCommand(name = "generate") {
    override fun run(): Unit = runBlocking {
        CloseableInjector(Guice.createInjector(ArchiveModule)).use { injector ->
            val importer = injector.getInstance(NameImporter::class.java)

            val names = mutableSetOf<String>()

            for (x in 0..99) {
                for (z in 0..255) {
                    for (prefix in PREFIXES) {
                        names += "$prefix${x}_$z"
                    }
                }
            }

            importer.import(names)
        }
    }

    private companion object {
        private val PREFIXES = setOf("m", "um", "l", "ul", "n", "e")
    }
}
