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
import org.openrs2.patcher.transform.RightClickTransformer
import org.openrs2.patcher.transform.TypoTransformer

public object PatcherModule : AbstractModule() {
    override fun configure() {
        install(AsmModule)
        install(ConfigModule)
        install(CryptoModule)

        val binder = Multibinder.newSetBinder(binder(), Transformer::class.java, PatcherQualifier::class.java)
        binder.addBinding().to(BufferSizeTransformer::class.java)
        binder.addBinding().to(CachePathTransformer::class.java)
        binder.addBinding().to(HostCheckTransformer::class.java)
        binder.addBinding().to(DomainTransformer::class.java)
        binder.addBinding().to(LoadLibraryTransformer::class.java)
        binder.addBinding().to(MacResizeTransformer::class.java)
        binder.addBinding().to(MemoryAllocationTransformer::class.java)
        binder.addBinding().to(NameTransformer::class.java)
        binder.addBinding().to(PlatformDetectionTransformer::class.java)
        binder.addBinding().to(PublicKeyTransformer::class.java)
        binder.addBinding().to(RightClickTransformer::class.java)
        binder.addBinding().to(TypoTransformer::class.java)
        binder.addBinding().to(HighDpiTransformer::class.java)
        binder.addBinding().to(InvalidKeyTransformer::class.java)
    }
}
