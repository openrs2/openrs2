package org.openrs2.bundler

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import org.openrs2.asm.transform.Transformer
import org.openrs2.bundler.transform.BufferSizeTransformer
import org.openrs2.bundler.transform.CachePathTransformer
import org.openrs2.bundler.transform.DomainTransformer
import org.openrs2.bundler.transform.HighDpiTransformer
import org.openrs2.bundler.transform.HostCheckTransformer
import org.openrs2.bundler.transform.LoadLibraryTransformer
import org.openrs2.bundler.transform.MacResizeTransformer
import org.openrs2.bundler.transform.MemoryAllocationTransformer
import org.openrs2.bundler.transform.NameTransformer
import org.openrs2.bundler.transform.PlatformDetectionTransformer
import org.openrs2.bundler.transform.PublicKeyTransformer
import org.openrs2.bundler.transform.RightClickTransformer
import org.openrs2.bundler.transform.TypoTransformer
import org.openrs2.conf.ConfigModule
import org.openrs2.crypto.CryptoModule

public object BundlerModule : AbstractModule() {
    override fun configure() {
        install(ConfigModule)
        install(CryptoModule)

        val binder = Multibinder.newSetBinder(binder(), Transformer::class.java, BundlerQualifier::class.java)
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
    }
}
