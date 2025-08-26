package org.openrs2.net

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import org.openrs2.buffer.BufferModule

public object NetworkModule : AbstractModule() {
    override fun configure() {
        install(BufferModule)

        bind(Transport::class.java)
            .toProvider(TransportProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }
}
