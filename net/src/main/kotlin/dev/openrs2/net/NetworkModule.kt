package dev.openrs2.net

import com.google.inject.AbstractModule
import org.openrs2.buffer.BufferModule

public object NetworkModule : AbstractModule() {
    override fun configure() {
        install(BufferModule)
    }
}
