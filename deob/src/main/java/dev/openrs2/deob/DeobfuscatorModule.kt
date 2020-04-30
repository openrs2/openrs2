package dev.openrs2.deob

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.bundler.BundlerModule
import dev.openrs2.bundler.transform.ResourceTransformer
import dev.openrs2.deob.transform.BitShiftTransformer
import dev.openrs2.deob.transform.BitwiseOpTransformer
import dev.openrs2.deob.transform.BundlerTransformer
import dev.openrs2.deob.transform.CanvasTransformer
import dev.openrs2.deob.transform.ClassLiteralTransformer
import dev.openrs2.deob.transform.ConstantArgTransformer
import dev.openrs2.deob.transform.CounterTransformer
import dev.openrs2.deob.transform.EmptyClassTransformer
import dev.openrs2.deob.transform.ExceptionTracingTransformer
import dev.openrs2.deob.transform.FernflowerExceptionTransformer
import dev.openrs2.deob.transform.FieldOrderTransformer
import dev.openrs2.deob.transform.FinalTransformer
import dev.openrs2.deob.transform.InvokeSpecialTransformer
import dev.openrs2.deob.transform.MethodOrderTransformer
import dev.openrs2.deob.transform.MonitorTransformer
import dev.openrs2.deob.transform.OpaquePredicateTransformer
import dev.openrs2.deob.transform.OriginalNameTransformer
import dev.openrs2.deob.transform.OriginalPcRestoreTransformer
import dev.openrs2.deob.transform.OriginalPcSaveTransformer
import dev.openrs2.deob.transform.OverrideTransformer
import dev.openrs2.deob.transform.RedundantGotoTransformer
import dev.openrs2.deob.transform.RemapTransformer
import dev.openrs2.deob.transform.ResetTransformer
import dev.openrs2.deob.transform.StaticScramblingTransformer
import dev.openrs2.deob.transform.UnusedArgTransformer
import dev.openrs2.deob.transform.UnusedLocalTransformer
import dev.openrs2.deob.transform.UnusedMethodTransformer
import dev.openrs2.deob.transform.VisibilityTransformer

object DeobfuscatorModule : AbstractModule() {
    override fun configure() {
        install(BundlerModule)

        val binder = Multibinder.newSetBinder(binder(), Transformer::class.java, DeobfuscatorQualifier::class.java)
        binder.addBinding().to(OriginalPcSaveTransformer::class.java)
        binder.addBinding().to(OriginalNameTransformer::class.java)
        binder.addBinding().to(BundlerTransformer::class.java)
        binder.addBinding().to(ResourceTransformer::class.java)
        binder.addBinding().to(OpaquePredicateTransformer::class.java)
        binder.addBinding().to(ExceptionTracingTransformer::class.java)
        binder.addBinding().to(MonitorTransformer::class.java)
        binder.addBinding().to(BitShiftTransformer::class.java)
        binder.addBinding().to(CanvasTransformer::class.java)
        binder.addBinding().to(FieldOrderTransformer::class.java)
        binder.addBinding().to(BitwiseOpTransformer::class.java)
        binder.addBinding().to(RemapTransformer::class.java)
        binder.addBinding().to(ConstantArgTransformer::class.java)
        binder.addBinding().to(UnusedLocalTransformer::class.java)
        binder.addBinding().to(UnusedMethodTransformer::class.java)
        binder.addBinding().to(UnusedArgTransformer::class.java)
        binder.addBinding().to(CounterTransformer::class.java)
        binder.addBinding().to(ResetTransformer::class.java)
        binder.addBinding().to(ClassLiteralTransformer::class.java)
        binder.addBinding().to(InvokeSpecialTransformer::class.java)
        binder.addBinding().to(StaticScramblingTransformer::class.java)
        binder.addBinding().to(EmptyClassTransformer::class.java)
        binder.addBinding().to(MethodOrderTransformer::class.java)
        binder.addBinding().to(VisibilityTransformer::class.java)
        binder.addBinding().to(FinalTransformer::class.java)
        binder.addBinding().to(OverrideTransformer::class.java)
        binder.addBinding().to(RedundantGotoTransformer::class.java)
        binder.addBinding().to(OriginalPcRestoreTransformer::class.java)
        binder.addBinding().to(FernflowerExceptionTransformer::class.java)
    }
}
