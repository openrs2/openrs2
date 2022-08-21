package org.openrs2.cache.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import com.google.inject.Guice
import io.netty.buffer.ByteBufAllocator
import org.openrs2.cache.CacheModule
import org.openrs2.cache.OpenNxtStore
import org.openrs2.cache.Store
import org.openrs2.inject.CloseableInjector

public class OpenNxtUnpackCommand : CliktCommand(name = "unpack-opennxt") {
    private val input by argument().path(mustExist = true, canBeFile = false, mustBeReadable = true)
    private val output by argument().path(canBeFile = false, mustBeReadable = true, mustBeWritable = true)

    override fun run() {
        CloseableInjector(Guice.createInjector(CacheModule)).use { injector ->
            val alloc = injector.getInstance(ByteBufAllocator::class.java)

            Store.open(output, alloc).use { store ->
                OpenNxtStore.unpack(input, store)
            }
        }
    }
}
