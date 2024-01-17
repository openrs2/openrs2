package org.openrs2.patcher

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import org.openrs2.asm.AsmModule
import org.openrs2.asm.transform.Transformer
import org.openrs2.conf.ConfigModule
import org.openrs2.crypto.CryptoModule
import org.openrs2.patcher.transform.BufferSizeTransformer
import org.openrs2.patcher.transform.CachePathTransformer
import org.openrs2.patcher.transform.DomainTransformer
import org.openrs2.patcher.transform.HighDpiTransformer
import org.openrs2.patcher.transform.HostCheckTransformer
import org.openrs2.patcher.transform.InvalidKeyTransformer
import org.openrs2.patcher.transform.LoadLibraryTransformer
import org.openrs2.patcher.transform.MacResizeTransformer
import org.openrs2.patcher.transform.MemoryAllocationTransformer
import org.openrs2.patcher.transform.NameTransformer
import org.openrs2.patcher.transform.PlatformDetectionTransformer
import org.openrs2.patcher.transform.PublicKeyTransformer
import org.openrs2.patcher.transform.ResourceTransformer
import org.openrs2.patcher.transform.RightClickTransformer
import org.openrs2.patcher.transform.TypoTransformer
import java.nio.file.Files
import java.nio.file.Paths

public object PatcherModule : AbstractModule() {
    override fun configure() {
        install(AsmModule)
        install(ConfigModule)
        install(CryptoModule)

        val binder = Multibinder.newSetBinder(binder(), Transformer::class.java, PatcherQualifier::class.java)

        if (Files.notExists(Paths.get("share", "deob", "patcher.transformers.list"))) {
            println("No patcher transformers")
            return
        }

        // would be nice to read this from yaml, unfortunately profile.yaml is not available until the module runs
        val transformers = Files.readAllLines(Paths.get("share/deob/patcher.transformers.list"))
        for (transformer in transformers) {
            if (transformer.isEmpty()) {
                continue
            }

            binder.addBinding().to(when (transformer) {
                "BufferSize" -> BufferSizeTransformer::class.java
                "CachePath" -> CachePathTransformer::class.java
                "HostCheck" -> HostCheckTransformer::class.java
                "Domain" -> DomainTransformer::class.java
                "LoadLibrary" -> LoadLibraryTransformer::class.java
                "MacResize" -> MacResizeTransformer::class.java
                "MemoryAllocation" -> MemoryAllocationTransformer::class.java
                "Name" -> NameTransformer::class.java
                "PlatformDetection" -> PlatformDetectionTransformer::class.java
                "PublicKey" -> PublicKeyTransformer::class.java
                "RightClick" -> RightClickTransformer::class.java
                "Typo" -> TypoTransformer::class.java
                "HighDpi" -> HighDpiTransformer::class.java
                "InvalidKey" -> InvalidKeyTransformer::class.java
                "Resource" -> ResourceTransformer::class.java
                else -> throw IllegalArgumentException("Unknown patcher transformer: $transformer")
            })
        }
    }
}
