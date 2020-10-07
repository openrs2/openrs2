package org.openrs2.deob.processor

import com.google.inject.AbstractModule
import org.openrs2.deob.util.DeobfuscatorUtilModule
import org.openrs2.yaml.YamlModule

public object DeobfuscatorProcessorModule : AbstractModule() {
    override fun configure() {
        install(DeobfuscatorUtilModule)
        install(YamlModule)
    }
}
