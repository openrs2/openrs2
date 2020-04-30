package dev.openrs2.bundler

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.bundler.transform.BufferSizeTransformer
import dev.openrs2.bundler.transform.CachePathTransformer
import dev.openrs2.bundler.transform.DomainTransformer
import dev.openrs2.bundler.transform.HostCheckTransformer
import dev.openrs2.bundler.transform.LoadLibraryTransformer
import dev.openrs2.bundler.transform.MacResizeTransformer
import dev.openrs2.bundler.transform.NameTransformer
import dev.openrs2.bundler.transform.PlatformDetectionTransformer
import dev.openrs2.bundler.transform.PublicKeyTransformer
import dev.openrs2.bundler.transform.RightClickTransformer
import dev.openrs2.bundler.transform.TypoTransformer
import dev.openrs2.conf.ConfigModule
import dev.openrs2.crypto.CryptoModule

object BundlerModule : AbstractModule() {
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
        binder.addBinding().to(NameTransformer::class.java)
        binder.addBinding().to(PlatformDetectionTransformer::class.java)
        binder.addBinding().to(PublicKeyTransformer::class.java)
        binder.addBinding().to(RightClickTransformer::class.java)
        binder.addBinding().to(TypoTransformer::class.java)
    }
}
