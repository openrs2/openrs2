package org.openrs2.archive.key

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule

public class BruteForceCommand : CliktCommand(name = "brute-force") {
    override fun run(): Unit = runBlocking {
        val injector = Guice.createInjector(ArchiveModule)
        val bruteForcer = injector.getInstance(KeyBruteForcer::class.java)

        bruteForcer.bruteForce()
    }
}
