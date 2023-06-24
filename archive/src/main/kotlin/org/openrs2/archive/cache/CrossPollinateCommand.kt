package org.openrs2.archive.cache

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.inject.CloseableInjector

public class CrossPollinateCommand : CliktCommand(name = "cross-pollinate") {
    override fun run(): Unit = runBlocking {
        CloseableInjector(Guice.createInjector(ArchiveModule)).use { injector ->
            val crossPollinator = injector.getInstance(CrossPollinator::class.java)
            crossPollinator.crossPollinate()
        }
    }
}
