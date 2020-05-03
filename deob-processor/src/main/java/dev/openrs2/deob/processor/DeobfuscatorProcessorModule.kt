package dev.openrs2.deob.processor

import com.google.inject.AbstractModule
import dev.openrs2.deob.map.DeobfuscatorMapModule
import dev.openrs2.yaml.YamlModule

object DeobfuscatorProcessorModule : AbstractModule() {
    override fun configure() {
        install(DeobfuscatorMapModule)
        install(YamlModule)
    }
}
